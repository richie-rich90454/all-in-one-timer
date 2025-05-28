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
        $("#stopwatch-h").text(time.h+":");
        $("#stopwatch-m").text(time.m+":");
        $("#stopwatch-s").text(time.s);
        $("#stopwatch-ms").text("."+time.ms);
    }
    function updateCountdown(){
        let remaining=endTime-performance.now();
        if (remaining<=0){
            clearInterval(countdownInterval);
            $("#countdown-h").text("00:");
            $("#countdown-m").text("00:");
            $("#countdown-s").text("00");
            $("#countdown-ms").text(".000");
            flashBackground();
            playOverspeedAlert();
            $(document).prop("title", "Time is up");
            setTimeout(function (){
                $(document).prop("title", "Time");
            }, 5000);
        }
        else{
            let time=formatTimeWithMs(remaining);
            $("#countdown-h").text(time.h+":");
            $("#countdown-m").text(time.m+":");
            $("#countdown-s").text(time.s);
            $("#countdown-ms").text("."+time.ms);
        }
    }
    function updateLocalTime(){
        let currentTime=new Date();
        let hours=currentTime.getHours();
        let minutes=currentTime.getMinutes();
        let seconds=currentTime.getSeconds();
        let ms=currentTime.getMilliseconds();
        $("#time-h").text(formatTime(hours)+":");
        $("#time-m").text(formatTime(minutes)+":");
        $("#time-s").text(formatTime(seconds));
        $("#time-ms").text("."+ms.toString().padStart(3, "0"));
        let offset=currentTime.getTimezoneOffset();
        let hoursOffset=Math.floor(Math.abs(offset)/60);
        let minutesOffset=Math.abs(offset)%60;
        let sign=offset>0?"-":"+";
        let timezoneStr=`(UTC${sign}${hoursOffset}${minutesOffset?":"+minutesOffset:""})`;
        $("#time-zone").text(timezoneStr);
    }
    $("#start-stopwatch").on("click", function (){
        if (swRun){
            clearInterval(stopwatchInterval);
            startedTime=performance.now()-startTime;
            $(this).text("START");
        }
        else{
            startTime=performance.now()-startedTime;
            stopwatchInterval=setInterval(updateStopwatch, 10);
            $(this).text("STOP");
        }
        swRun=!swRun;
    });
    $("#reset-stopwatch").on("click", function (){
        clearInterval(stopwatchInterval);
        startedTime=0;
        $("#stopwatch-h").text("00:");
        $("#stopwatch-m").text("00:");
        $("#stopwatch-s").text("00");
        $("#stopwatch-ms").text(".000");
        $("#start-stopwatch").text("START");
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
        let audioCtx=window.audioCtx||new (window.AudioContext||window.webkitAudioContext)();
        window.audioCtx=audioCtx;
        let playToneSequence=()=>{
            let now=audioCtx.currentTime;
            let toneCount=12;
            let toneDuration=.3;
            let spacing=.45;
            for (let i=0;i<toneCount;i++){
                let oscillator=audioCtx.createOscillator();
                oscillator.type="sine";
                oscillator.frequency.setValueAtTime(432, now+i*spacing);
                oscillator.connect(audioCtx.destination);
                oscillator.start(now+i*spacing);
                oscillator.stop(now+i*spacing+toneDuration);
                oscillator.onended=()=>oscillator.disconnect();
            }
        };
        if (audioCtx.state=="suspended"){
            audioCtx.resume().then(playToneSequence).catch(error=>{
                console.error("Failed to resume AudioContext:", error);
            });
        }
        else{
            playToneSequence();
        }
    }
    $("#timer-setter").on("click", function (){
        clearInterval(countdownInterval);
        if (!($("#timer-hour").val().trim()==""&&$("#timer-minute").val().trim()==""&&$("#timer-seconds").val().trim()=="")){
            let hours=parseInt($("#timer-hour").val())||0;
            let minutes=parseInt($("#timer-minute").val())||0;
            let seconds=parseInt($("#timer-seconds").val())||0;
            if (hours<0){
                hours=0;
                $("#timer-hour").val(0);
            }
            if (minutes<0){
                minutes=0;
                $("#timer-minute").val(0);
            }
            else if (minutes>59){
                minutes=59;
                $("#timer-minute").val(59);
            }
            if (seconds<0){
                seconds=0;
                $("#timer-seconds").val(0);
            }
            else if (seconds>59){
                seconds=59;
                $("#timer-seconds").val(59);
            }
            let duration=(hours*3600+minutes*60+seconds)*1000;
            endTime=performance.now()+duration;
            updateCountdown();
            countdownInterval=setInterval(updateCountdown, 10);
        }
    });
    setInterval(updateLocalTime, 50);
    updateLocalTime();
    $(document).on("contextmenu", (event)=>event.preventDefault());
    $(document).on("keydown", function (event){
        if (event.keyCode==123||(event.ctrlKey&&event.shiftKey&&event.keyCode==73)||(event.ctrlKey&&event.shiftKey&&event.keyCode==74)||(event.ctrlKey&&event.keyCode==85)){
            event.preventDefault();
        }
    });
});