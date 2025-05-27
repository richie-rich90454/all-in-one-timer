$(document).ready(function(){
    let swRun=false;
    let startTime;
    let stopwatchInterval;
    let endTime;
    let countdownInterval;
    let audioCtx;
    let startedTime=0;
    function formatTime(time){
        return time<10?"0"+time:time;
    }
    function formatTimeWithMs(totalMs){
        let hours=Math.floor(totalMs/3600000);
        let minutes=Math.floor((totalMs%3600000)/60000);
        let seconds=Math.floor((totalMs%60000)/1000);
        let ms=Math.floor(totalMs%1000);
        return{
            h: hours.toString().padStart(2, "0"),
            m: minutes.toString().padStart(2, "0"),
            s: seconds.toString().padStart(2, "0"),
            ms: ms.toString().padStart(3, "0")
        };
    }
    function updateStopwatch(){
        let elapsed=performance.now()-startTime;
        let time=formatTimeWithMs(elapsed);
        document.getElementById("stopwatch-h").textContent=time.h+":";
        document.getElementById("stopwatch-m").textContent=time.m+":";
        document.getElementById("stopwatch-s").textContent=time.s;
        document.getElementById("stopwatch-ms").textContent="."+time.ms;
    }
    function updateCountdown(){
        let remaining=endTime-performance.now();
        if (remaining<=0){
            clearInterval(countdownInterval);
            document.getElementById("countdown-h").textContent="00:";
            document.getElementById("countdown-m").textContent="00:";
            document.getElementById("countdown-s").textContent="00";
            document.getElementById("countdown-ms").textContent=".000";
            flashBackground();
            playOverspeedAlert();
            document.title="Time is up";
            setTimeout(function (){
                document.title="Time";
            }, 5000);
        }
        else{
            let time=formatTimeWithMs(remaining);
            document.getElementById("countdown-h").textContent=time.h+":";
            document.getElementById("countdown-m").textContent=time.m+":";
            document.getElementById("countdown-s").textContent=time.s;
            document.getElementById("countdown-ms").textContent="."+time.ms;
        }
    }
    function updateLocalTime(){
        let currentTime=new Date();
        let hours=currentTime.getHours();
        let minutes=currentTime.getMinutes();
        let seconds=currentTime.getSeconds();
        let ms=currentTime.getMilliseconds();
        document.getElementById("time-h").textContent=formatTime(hours)+":";
        document.getElementById("time-m").textContent=formatTime(minutes)+":";
        document.getElementById("time-s").textContent=formatTime(seconds);
        document.getElementById("time-ms").textContent="."+ms.toString().padStart(3, "0");
        let offset=currentTime.getTimezoneOffset();
        let hoursOffset=Math.floor(Math.abs(offset)/60);
        let minutesOffset=Math.abs(offset)%60;
        let sign=offset>0?"-":"+";
        let timezoneStr=`(UTC${sign}${hoursOffset}${minutesOffset?":"+minutesOffset:""})`;
        document.getElementById("time-zone").textContent=timezoneStr;
    }
    document.getElementById("start-stopwatch").addEventListener("click", function (){
        if (swRun){
            clearInterval(stopwatchInterval);
            startedTime=performance.now()-startTime;
            this.textContent="START";
        }
        else{
            startTime=performance.now()-startedTime;
            stopwatchInterval=setInterval(updateStopwatch, 10);
            this.textContent="STOP";
        }
        swRun=!swRun;
    });
    document.getElementById("reset-stopwatch").addEventListener("click", function (){
        clearInterval(stopwatchInterval);
        startedTime=0;
        document.getElementById("stopwatch-h").textContent="00:";
        document.getElementById("stopwatch-m").textContent="00:";
        document.getElementById("stopwatch-s").textContent="00";
        document.getElementById("stopwatch-ms").textContent=".000";
        document.getElementById("start-stopwatch").textContent="START";
        swRun=false;
    });
    function flashBackground(){
        let cdBox=$("#countdown-box");
        cdBox.addClass("flash");
        cdBox.effect("shake",{times: 3}, 1000);
        setTimeout(function (){
            cdBox.removeClass("flash");
        }, 5000);
    }
    function playOverspeedAlert(){
        if (!audioCtx){
            audioCtx=new (window.AudioContext||window.webkitAudioContext)();
        }
        let resumeAndPlay=()=>{
            let now=audioCtx.currentTime;
            for (let i=0;i<5;i++){
                let oscillator=audioCtx.createOscillator();
                oscillator.type="square";
                oscillator.frequency.setValueAtTime(i%2==0?880:660, now+i);
                oscillator.connect(audioCtx.destination);
                oscillator.start(now+i);
                oscillator.stop(now+i+0.6);
                oscillator.onended=()=>oscillator.disconnect();
            }
        };
        if (audioCtx.state=="suspended"){
            audioCtx.resume().then(resumeAndPlay);
        }
        else{
            resumeAndPlay();
        }
    }
    document.getElementById("timer-setter").addEventListener("click", function (){
        clearInterval(countdownInterval);
        if (!(document.getElementById("timer-hour").value.trim()==""&&document.getElementById("timer-minute").value.trim()==""&&document.getElementById("timer-seconds").value.trim()=="")){
            let hours=parseInt(document.getElementById("timer-hour").value)||0;
            let minutes=parseInt(document.getElementById("timer-minute").value)||0;
            let seconds=parseInt(document.getElementById("timer-seconds").value)||0;
            if (hours<0){
                hours=0;
                document.getElementById("timer-hour").value=0;
            }
            if (minutes<0){
                minutes=0;
                document.getElementById("timer-minute").value=0;
            }
            else if (minutes>59){
                minutes=59;
                document.getElementById("timer-minute").value=59;
            }
            if (seconds<0){
                seconds=0;
                document.getElementById("timer-seconds").value=0;
            }
            else if (seconds>59){
                seconds=59;
                document.getElementById("timer-seconds").value=59;
            }
            let duration=(hours*3600+minutes*60+seconds)*1000;
            endTime=performance.now()+duration;
            updateCountdown();
            countdownInterval=setInterval(updateCountdown, 10);
        }
    });
    setInterval(updateLocalTime, 50);
    updateLocalTime();
    document.addEventListener("contextmenu", (event)=>event.preventDefault());
    document.addEventListener("keydown", function (event){
        if (event.keyCode==123||(event.ctrlKey&&event.shiftKey&&event.keyCode==73)||
            (event.ctrlKey&&event.shiftKey&&event.keyCode==74)||(event.ctrlKey&&event.keyCode==85)){
            event.preventDefault();
        }
    });
});