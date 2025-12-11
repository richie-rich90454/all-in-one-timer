# All-in-One Timer Tool
A lightweight, responsive web timer suite that includes:
1. **Stopwatch** - Live stopwatch with start, stop, and reset.
2. **Countdown Timer** - Custom countdown with flash and alert.
3. **Current Time Display** - Local time with UTC offset.
Served with a no-cache Fastify server. Just run `node server.js` and go!
---
## Features
### Stopwatch
* Start/Stop toggle button
* Reset functionality
* Millisecond precision
### Countdown Timer
* Input fields for Hours / Minutes / Seconds
* Visual flash and audible alert on time-up
* Title change on completion
### Current Time
* Updates every 50ms
* Displays `(UTC+ or -offset)` from user's local timezone
### Tech Stack
* HTML/CSS/JavaScript (vanilla + jQuery)
* Web Audio API for sound alert
* Fastify for local static hosting
---
## Setup
1. **Clone the repository**
```bash
git clone https://github.com/richie-rich90454/all-in-one-timer.git
cd all-in-one-timer
```
2. **Install dependencies**
```bash
npm install
```
3. **Run the server**
```bash
node server.js
```
4. **View in browser**
   Open [http://localhost:6006](http://localhost:6006)
---
## Usage
### Stopwatch
* `START` to begin
* `STOP` to pause
* `RESET` to clear
### Countdown
* Set hours, minutes, seconds
* Click `Set Countdown`
* Watch timer with alert and flash
### Current Time
* Live digital clock with milliseconds
* Shows local UTC offset
---
## Project Structure
```
.
├── favicon.png           # App icon
├── index.html            # HTML UI layout
├── script.js             # Timer logic (stopwatch, countdown, clock)
├── server.js             # Fastify config
├── package.json          # Node dependencies
└── README.md             # You're reading it
```
---
## Credits
* **Developer**: [@richie-rich90454](https://github.com/richie-rich90454)
* **Font**: Google Fonts - Noto Sans
* **Audio**: Web Audio API
* **Framework**: Fastify + @fastify/static
* **Sites**: [richardsblogs.com](https://www.richardsblogs.com), [biszweb.club](https://www.biszweb.club)
---
## License
Apache-2.0 License - See [LICENSE](./LICENSE) for details.