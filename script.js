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
    }
    updateCDDisplay();
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
        cdBox.style.backgroundColor="#F0F0F";
    },5000);
}
function parseHMS(seconds){
    let h=Math.floor(seconds/3600);
    let m=Math.floor((seconds%3600)/60);
    let s=seconds%60;
    return {h,m,s}
}
document.getElementById("timer-setter").addEventListener("click",function(){
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