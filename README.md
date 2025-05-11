# All-in-One Timer Tool
A simple, responsive web application that provides three timing utilities in one place:
1. **Stopwatch** – Start, stop, and reset a live stopwatch.
2. **Current Time** – Displays your local current time (hours, minutes, seconds) and UTC offset.
3. **Countdown Timer** – Set a custom hours/minutes/seconds countdown, with flashing background and audible alert when time is up.
This repository serves the static HTML/CSS/JS via a Fastify server. Just run `node server.js` to get started!
---
## Features
* **Modular Design**
  * Separate `index.html` and `script.js` files
  * Inline CSS for quick prototyping, using Google’s **Noto Sans SC** font
* **Stopwatch**
  * Start/stop toggle button
  * Reset button
  * Hours : Minutes : Seconds display
* **Current Time**
  * Live update every second
  * Shows local timezone offset in UTC format
* **Countdown**
  * Input fields for hours, minutes, and seconds
  * “Set Countdown” button
  * Flashing background and document title change when time is up
  * Audible alert using Web Audio API
* **Server**
  * Built with [Fastify](https://www.fastify.io/)
  * Serves static files from the repository root
  * Disables caching for development convenience
  * Custom 404 and 500 error handlers
---
## Installation
1. **Clone the repository**
   ```bash
   git clone https://github.com/richie-rich90454/all-in-one-timer.git
   cd all-in-one-timer
   ```
2. **Install dependencies**
   ```bash
   npm install
   ```
3. **Start the server**

   ```bash
   node server.js
   ```
4. **Open your browser**
   Navigate to `http://localhost:6006`
---
## Usage
* **Stopwatch**
  * Click **START STOPWATCH** to begin
  * Click **STOP STOPWATCH** to pause
  * Click **RESET** to zero out the stopwatch
* **Current Time**
  * Automatically updates every second
  * Displays `(UTC±offset)` based on your locale
* **Countdown**
  * Enter desired hours/minutes/seconds
  * Click **Set Countdown** to begin
  * When it reaches zero:
    * Background flashes for 5 seconds
    * Document title changes to “TIME IS UP” briefly
    * A sequence of beeps plays via the Web Audio API
---
## Project Structure
```
.
├── favicon.png           # Site favicon
├── index.html            # Main HTML file
├── script.js             # All timer logic & client-side code
├── server.js             # Fastify server configuration
├── package.json          # Dependency manifest
└── README.md             # You are here!
```
---
## Credits
* **Author**: richie-rich90454
* **Font**: “Noto Sans SC” from Google Fonts
* **Server**: [Fastify](https://www.fastify.io/) and [@fastify/static](https://github.com/fastify/fastify-static)
* **Audio**: Web Audio API
* **Favicon**: `favicon.png` (make sure to credit or replace with your own icon if needed)
---
## License
This project is open-source under the **Apache-2.0 License**. See [LICENSE](./LICENSE) for details.
---