"""Jarvis for Windows 10/11. Fully offline voice assistant: no API key, no accounts, nothing saved to disk."""
import os, sys, re, json, queue, threading, time, ctypes, subprocess, webbrowser, datetime, difflib, urllib.parse, math, random
import tkinter as tk

BG, PANEL, LINE, CY, TX, MUT, GOLD, OFF = "#050B14", "#0A1626", "#12304D", "#3EE6FF", "#CFEFFF", "#6F93AA", "#FFB347", "#4A5A66"
BASE = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
MODEL = os.path.join(BASE, "model")
# Rule: never touch money. Anything about payments/banking is refused locally.
FIN = re.compile(r"\b(pay|payment|transaction|transfer|send money|money|upi|bank|banking|account number|card|cvv|pin|otp|gpay|google pay|phonepe|paytm|bhim|netbanking|loan|wallet|paypal|balance|debit|credit)\b")
APPS = {"notepad": "notepad", "calculator": "calc", "paint": "mspaint", "file explorer": "explorer", "explorer": "explorer",
        "settings": "ms-settings:", "task manager": "taskmgr", "command prompt": "cmd", "chrome": "chrome", "edge": "msedge",
        "word": "winword", "excel": "excel", "powerpoint": "powerpnt", "whatsapp": "whatsapp:", "camera": "microsoft.windows.camera:",
        "store": "ms-windows-store:", "spotify": "spotify", "vs code": "code", "visual studio code": "code"}
SITES = {"youtube": "https://www.youtube.com", "google": "https://www.google.com", "gmail": "https://mail.google.com",
         "maps": "https://maps.google.com", "github": "https://github.com", "wikipedia": "https://wikipedia.org",
         "netflix": "https://www.netflix.com", "instagram": "https://www.instagram.com", "whatsapp web": "https://web.whatsapp.com"}
VK = dict(ALT=0x12, CTRL=0x11, SHIFT=0x10, WIN=0x5B, TAB=0x09, W=0x57, T=0x54, D=0x44, SNAP=0x2C, VUP=0xAF, VDN=0xAE, MUTE=0xAD)
STATE_TXT = ["HANDS-FREE OFF", "LISTENING FOR “HEY JARVIS”", "LISTENING…", "SPEAKING"]


def key(*names):
    u = ctypes.windll.user32; ks = [VK[n] for n in names]
    for k in ks: u.keybd_event(k, 0, 0, 0)
    for k in reversed(ks): u.keybd_event(k, 0, 2, 0)


def is_wake(tok):
    return difflib.SequenceMatcher(None, tok, "jarvis").ratio() >= 0.75


class Engine:
    def __init__(self, ui):
        self.ui = ui; self.on = True; self.speaking = False; self.awake = 0.0; self.pending = None
        self.audio = queue.Queue(); self.sayq = queue.Queue()
        threading.Thread(target=self.talker, daemon=True).start()
        threading.Thread(target=self.listen, daemon=True).start()

    # ---------- speech out ----------
    def speak(self, text):
        self.ui.post("log", 0, text); self.speaking = True; self.ui.post("state", 3); self.sayq.put(text)

    def talker(self):
        import pyttsx3
        while True:
            t = self.sayq.get()
            try:
                e = pyttsx3.init(); e.setProperty("rate", 175)
                for v in e.getProperty("voices"):
                    if "david" in v.name.lower() or "ravi" in v.name.lower(): e.setProperty("voice", v.id); break
                e.say(t); e.runAndWait(); e.stop()
            except Exception:
                pass
            if self.sayq.empty(): self.speaking = False

    # ---------- speech in (offline Vosk) ----------
    def listen(self):
        try:
            import sounddevice as sd
            from vosk import Model, KaldiRecognizer, SetLogLevel
            SetLogLevel(-1); rec = KaldiRecognizer(Model(MODEL), 16000)
            stream = sd.RawInputStream(samplerate=16000, blocksize=4000, dtype="int16", channels=1, callback=lambda d, f, t, s: self.audio.put(bytes(d)))
            stream.start()
        except Exception as ex:
            self.ui.post("log", 0, "I couldn’t start the microphone or voice model. You can still type commands. (%s)" % ex); return
        paused = True; self.ui.post("state", 1)
        while True:
            data = self.audio.get()
            if not self.on or self.speaking:
                if not paused: paused = True; self.ui.post("state", 0 if not self.on else 3)
                continue
            if paused:
                paused = False; rec.Reset(); self.ui.post("state", 2 if time.time() < self.awake else 1)
            if rec.AcceptWaveform(data):
                text = json.loads(rec.Result()).get("text", "").strip()
                if text: self.process(text)
                if not self.speaking: self.ui.post("state", 2 if time.time() < self.awake else 1)

    def process(self, text):
        toks = text.split()
        for i, t in enumerate(toks[:3]):
            if is_wake(t):
                cmd = " ".join(toks[i + 1:])
                if cmd: self.awake = 0; self.command(cmd)
                else: self.awake = time.time() + 8; self.speak("Yes, sir?")
                return
        if time.time() < self.awake: self.awake = 0; self.command(text)

    def command(self, raw):
        self.ui.post("log", 1, raw)
        try: self.speak(self.run(raw))
        except Exception: self.speak("Sorry, something went wrong with that one.")

    # ---------- commands ----------
    def open_app(self, name):
        if name in SITES: webbrowser.open(SITES[name]); return "Opening %s." % name
        target = APPS.get(name)
        if target is None:
            if not re.fullmatch(r"[a-z0-9 ]{2,30}", name): return "I can’t open that."
            target = name
        try: os.startfile(target); return "Opening %s." % name
        except OSError: return "I couldn’t find an app called %s." % name

    def run(self, raw):
        l = re.sub(r"[?.!,]", "", raw.lower()).strip()
        if not l: return "I’m listening, sir."
        if self.pending:
            act, self.pending = self.pending, None
            return act() if re.search(r"\b(yes|confirm|do it|haan)\b", l) else "Cancelled."
        if FIN.search(l): return "I can’t help with payments, banking or money details. That keeps your finances private and safe."
        has = lambda *k: any(x in l for x in k)
        m = re.search(r"timer (?:for )?(\d+) ?(sec|min|hour)", l)
        if m:
            s = int(m.group(1)) * {"sec": 1, "min": 60, "hour": 3600}[m.group(2)]
            def done():
                import winsound; winsound.Beep(1000, 600); self.speak("Sir, your timer has finished.")
            threading.Timer(s, done).start(); return "Timer set."
        m = re.match(r"(?:open|launch|kholo|khol do) (.+)", l)
        if m: return self.open_app(m.group(1).strip())
        m = re.search(r"(?:whatsapp|message|text) \+?(\d[\d ]{7,14}) (.+)", l)
        if m:
            webbrowser.open("https://wa.me/%s?text=%s" % (m.group(1).replace(" ", ""), urllib.parse.quote(m.group(2))))
            return "Message drafted. Please press send in WhatsApp to confirm."
        m = re.search(r"(?:wikipedia|wiki) (.+)", l)
        if m: webbrowser.open("https://en.wikipedia.org/wiki/Special:Search?search=" + urllib.parse.quote(m.group(1))); return "Looking that up on Wikipedia."
        m = re.search(r"(?:search|google|look up) (?:for )?(.+)", l)
        if m: webbrowser.open("https://www.google.com/search?q=" + urllib.parse.quote(m.group(1))); return "Searching the web."
        if has("brightness", "roshni"):
            n = re.search(r"(\d+)", l); n = min(100, int(n.group(1))) if n else (85 if has("up", "increase", "badha", "more") else 30)
            r = subprocess.run(["powershell", "-NoProfile", "-Command", "(Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1,%d)" % n],
                               capture_output=True, creationflags=0x08000000, timeout=15)
            return "Brightness set to %d percent." % n if r.returncode == 0 else "This display doesn’t allow software brightness. That’s common on desktop monitors."
        if has("volume up", "louder", "increase volume", "volume badh", "awaaz badh"): [key("VUP") for _ in range(5)]; return "Volume up."
        if has("volume down", "quieter", "decrease volume", "volume kam", "awaaz kam"): [key("VDN") for _ in range(5)]; return "Volume down."
        if has("mute"): key("MUTE"); return "Done."
        if has("screenshot"): key("WIN", "SNAP"); return "Screenshot saved in your Pictures, Screenshots folder."
        if has("next tab"): key("CTRL", "TAB"); return "Next tab."
        if has("previous tab", "last tab"): key("CTRL", "SHIFT", "TAB"); return "Previous tab."
        if has("close tab"): key("CTRL", "W"); return "Tab closed."
        if has("new tab"): key("CTRL", "T"); return "New tab."
        if has("switch window", "next window", "alt tab", "switch app"): key("ALT", "TAB"); return "Switching window."
        if has("show desktop", "minimize all", "minimise all"): key("WIN", "D"); return "Showing the desktop."
        if re.search(r"\block\b", l): ctypes.windll.user32.LockWorkStation(); return "Locking your PC."
        if has("cancel shutdown", "abort shutdown", "cancel restart"): subprocess.run(["shutdown", "/a"], creationflags=0x08000000); return "Cancelled."
        if has("shut down", "shutdown", "power off", "turn off the computer", "turn off the pc"):
            self.pending = lambda: (subprocess.run(["shutdown", "/s", "/t", "10"], creationflags=0x08000000), "Shutting down in 10 seconds. Say cancel shutdown to stop.")[1]
            return "Are you sure you want to shut down? Say yes to confirm."
        if has("restart", "reboot"):
            self.pending = lambda: (subprocess.run(["shutdown", "/r", "/t", "10"], creationflags=0x08000000), "Restarting in 10 seconds. Say cancel restart to stop.")[1]
            return "Are you sure you want to restart? Say yes to confirm."
        if has("help", "what can you do"): return "Try: open Chrome, volume up, brightness 40, next tab, switch window, timer for 5 minutes, screenshot, lock, wikipedia a topic, or search anything."
        if re.fullmatch(r"(hi|hello|hey|namaste|good (morning|afternoon|evening))", l): return "Hello, sir. How can I help?"
        if has("who are you", "your name"): return "I am Jarvis, your private offline assistant. Everything runs on this PC."
        if has("thank"): return "Always a pleasure, sir."
        if has("what time", "the time", "samay"): return "It is " + datetime.datetime.now().strftime("%I:%M %p").lstrip("0") + "."
        if has("what date", "todays date", "what day", "aaj ki date"): return "Today is " + datetime.datetime.now().strftime("%A, %d %B %Y") + "."
        if has("joke"): return random.choice(["Why do programmers prefer dark mode? Because light attracts bugs.", "I would tell you a UDP joke, but you might not get it."])
        return "I don’t know that command. Say help to see available commands. For a web lookup, say search followed by your query."


class App:
    def __init__(self):
        try: ctypes.windll.shcore.SetProcessDpiAwareness(1)
        except Exception: pass
        self.q = queue.Queue(); self.state = 0; self.t = 0
        r = self.root = tk.Tk(); r.title("Jarvis"); r.geometry("440x760"); r.minsize(380, 600); r.configure(bg=BG)
        tk.Label(r, text="J A R V I S", font=("Segoe UI Light", 26), fg=CY, bg=BG).pack(pady=(18, 0))
        tk.Label(r, text="Say “Hey Jarvis”", font=("Segoe UI", 11), fg=MUT, bg=BG).pack()
        self.cv = tk.Canvas(r, width=220, height=220, bg=BG, highlightthickness=0); self.cv.pack(pady=8)
        self.st = tk.Label(r, text=STATE_TXT[0], font=("Segoe UI", 10), fg=CY, bg=BG); self.st.pack()
        bot = tk.Frame(r, bg=BG); bot.pack(side=tk.BOTTOM, fill=tk.X, padx=14, pady=12)
        self.ent = tk.Entry(bot, font=("Segoe UI", 11), bg=PANEL, fg=TX, insertbackground=TX, relief="flat", highlightthickness=1, highlightbackground=LINE, highlightcolor=CY)
        self.ent.pack(side=tk.LEFT, fill=tk.X, expand=True, ipady=8); self.ent.bind("<Return>", lambda e: self.send())
        tk.Button(bot, text="Send", command=self.send, bg=CY, fg=BG, relief="flat", font=("Segoe UI", 10, "bold"), padx=14).pack(side=tk.LEFT, padx=(8, 0), fill=tk.Y)
        self.tg = tk.Button(r, text="Turn off Jarvis", command=self.toggle, bg=PANEL, fg=CY, relief="flat", font=("Segoe UI", 10), pady=6)
        self.tg.pack(side=tk.BOTTOM, fill=tk.X, padx=14)
        fr = tk.Frame(r, bg=PANEL); fr.pack(fill=tk.BOTH, expand=True, padx=14, pady=8)
        self.log = tk.Text(fr, bg=PANEL, fg=TX, relief="flat", wrap="word", font=("Segoe UI", 10), state="disabled", padx=10, pady=8)
        self.log.pack(fill=tk.BOTH, expand=True); self.log.tag_configure("jv", foreground=CY, spacing3=6); self.log.tag_configure("me", foreground=TX, justify="right", spacing3=6)
        self.eng = Engine(self); self.draw(); self.pump()
        self.post("log", 0, "Systems online. Say “Hey Jarvis” and then your command, or type below.")

    def post(self, *a): self.q.put(a)

    def send(self):
        t = self.ent.get().strip(); self.ent.delete(0, "end")
        if t: threading.Thread(target=self.eng.command, args=(t,), daemon=True).start()

    def toggle(self):
        self.eng.on = not self.eng.on; self.tg.config(text="Turn off Jarvis" if self.eng.on else "Turn on Jarvis")
        if self.eng.on: self.eng.audio.queue.clear()

    def pump(self):
        try:
            while True:
                k, *a = self.q.get_nowait()
                if k == "log":
                    self.log.config(state="normal"); self.log.insert("end", ("Jarvis: " if a[0] == 0 else "You: ") + a[1] + "\n", "jv" if a[0] == 0 else "me")
                    self.log.config(state="disabled"); self.log.see("end")
                else: self.state = a[0]; self.st.config(text=STATE_TXT[a[0]])
        except queue.Empty: pass
        self.root.after(100, self.pump)

    @staticmethod
    def mix(c, f):
        v = [int(c[i:i + 2], 16) for i in (1, 3, 5)]; return "#%02x%02x%02x" % tuple(int(x + (255 - x) * f) for x in v)

    def draw(self):
        c = self.cv; c.delete("all"); self.t += 4; cx = cy = 110
        col = OFF if self.state == 0 else GOLD if self.state == 3 else CY
        c.create_oval(cx - 104, cy - 104, cx + 104, cy + 104, outline=col, width=1)
        for k in range(14): c.create_arc(cx - 88, cy - 88, cx + 88, cy + 88, start=self.t + k * 360 / 14, extent=14, style="arc", outline=col, width=2)
        for s in (0, 180): c.create_arc(cx - 70, cy - 70, cx + 70, cy + 70, start=-self.t * 1.5 + s, extent=80, style="arc", outline=col, width=4)
        pulse = 1 + 0.07 * math.sin(math.radians(self.t * (6 if self.state >= 2 else 3))); R = 50 * pulse
        for i in range(10):
            r = R * (1 - i / 10); c.create_oval(cx - r, cy - r, cx + r, cy + r, fill=self.mix(col, i / 9 * 0.95), outline="")
        self.root.after(33, self.draw)


if __name__ == "__main__":
    App().root.mainloop()
