let swInterval, cdInterval;
let swRun=false;
let swH=0, swM=0, swS=0;
let cdH=0, cdM=0, cdS=0;
let stopwatchH=document.getElementById("stopwatch-h");
let stopwatchM=document.getElementById("stopwatch-m");
let stopwatchS=document.getElementById("stopwatch-s");
let countdownH=document.getElementById("countdown-h");
let countdownM=document.getElementById("countdown-m");
let countdownS=document.getElementById("countdown-s");
let flashInterval;
let audioCtx;
function formatTime(time){
    if (time<10){
        return "0"+time;
    }
    else{
        return time;
    }
}
function updateSWDisplay(){
    stopwatchH.textContent=formatTime(swH)+":";
    stopwatchM.textContent=formatTime(swM)+":";
    stopwatchS.textContent=formatTime(swS);
}
function updateCDDisplay(){
    countdownH.textContent=formatTime(cdH)+":";
    countdownM.textContent=formatTime(cdM)+":";
    countdownS.textContent=formatTime(cdS);
}
function countdown(){
    if (cdS>0){
        cdS--;
    }
    else if (cdM>0){
        cdM--;
        cdS=59;
    }
    else if (cdH>0){
        cdH--;
        cdM=59;
        cdS=59;
    }
    if (cdH==0&&cdM==0&&cdS==0){
        clearInterval(cdInterval);
        flashBackground();
        document.title="TIME IS UP";
        setTimeout(function(){
            document.title="Time";
        },5000);
    }
    updateCDDisplay();
}
function updateLocalTime(){
    let currentTime=new Date();
    document.getElementById("time-h").textContent=formatTime(currentTime.getHours())+":";
    document.getElementById("time-m").textContent=formatTime(currentTime.getMinutes())+":";
    document.getElementById("time-s").textContent=formatTime(currentTime.getSeconds());
    if (currentTime.getTimezoneOffset()>0){
        document.getElementById("time-zone").textContent=formatTime(` (UTC-${currentTime.getTimezoneOffset()/60})`);
    }
    else{
        document.getElementById("time-zone").textContent=formatTime(` (UTC+${-currentTime.getTimezoneOffset()/60})`);
    }
}
document.getElementById("start-stopwatch").addEventListener("click", function(){
    if (swRun){
        clearInterval(swInterval);
        this.textContent="START STOPWATCH";
    }
    else{
        swInterval=setInterval(function(){
            swS++;
            if (swS==60){
                swS=0;
                swM++;
            }
            if (swM==60){
                swM=0;
                swH++;
            }
            updateSWDisplay();
        },1000);
        this.textContent="STOP STOPWATCH";
    }
    swRun=!swRun;
});
document.getElementById("reset-stopwatch").addEventListener("click", function(){
    clearInterval(swInterval);
    swH=0;
    swM=0;
    swS=0;
    updateSWDisplay();
    document.getElementById("start-stopwatch").textContent="START STOPWATCH";
    swRun=false;
});
function flashBackground(){
    let cdBox=document.getElementById("countdown-box");
    let flashing=true;
    setTimeout(playOverspeedAlert,500);
    flashInterval=setInterval(function(){
        if (flashing){
            cdBox.style.backgroundColor="#DE0000";
        }
        else{
            cdBox.style.backgroundColor="#F0F0F0";
        }
        flashing=!flashing;
    },500);
    setTimeout(function(){
        clearInterval(flashInterval);
        cdBox.style.backgroundColor="#F0F0F0";
    },5000);
}
function playOverspeedAlert(){
    if (!audioCtx){
        audioCtx=new (window.AudioContext||window.webkitAudioContext);
    }
    const resumeAndPlay=()=>{
        const now=audioCtx.currentTime;
        for (let i=0;i<5;i++){
            const oscillator=audioCtx.createOscillator();
            oscillator.type="square";
            oscillator.frequency.setValueAtTime(i%2==0?880:660,now+i);
            oscillator.connect(audioCtx.destination);
            oscillator.start(now+i);
            oscillator.stop(now+i+0.6);
        }
    };
    if (audioCtx.state=="suspended"){
        audioCtx.resume().then(resumeAndPlay);
    }
    else{
        resumeAndPlay();
    }
}
function parseHMS(seconds){
    let h=Math.floor(seconds/3600);
    let m=Math.floor((seconds%3600)/60);
    let s=seconds%60;
    return{h,m,s}
}
document.getElementById("timer-setter").addEventListener("click",function(){
    if (!audioCtx){
        audioCtx=new (window.AudioContext||window.webkitAudioContext)();
    }
    clearInterval(flashInterval);
    let hours=parseInt(document.getElementById("stopwatch-hour").value)||0;
    let minutes=parseInt(document.getElementById("stopwatch-minute").value)||0;
    let seconds=parseInt(document.getElementById("stopwatch-seconds").value)||0;
    if (hours<0){
        hours=0;
        document.getElementById("stopwatch-hour").value=0;
    }
    if (minutes<0){
        minutes=0;
        document.getElementById("stopwatch-minutes").value=0;
    }
    if (seconds<0){
        seconds=0;
        document.getElementById("stopwatch-seconds").value=0;
    }
    let parsedTime=parseHMS(hours*3600+minutes*60+seconds);
    cdH=parsedTime.h;
    cdM=parsedTime.m;
    cdS=parsedTime.s;
    updateCDDisplay();
    clearInterval(cdInterval);
    cdInterval=setInterval(countdown,1000);
});
setInterval(updateLocalTime, 1000);
updateLocalTime();
document.addEventListener("contextmenu", (event)=>event.preventDefault());
document.addEventListener("keydown", function(event){
    if (event.keyCode==123||(event.ctrlKey && event.shiftKey && event.keyCode==73)||(event.ctrlKey && event.shiftKey && event.keyCode==74)||(event.ctrlKey && event.keyCode==85)){
        event.preventDefault();
    }
});