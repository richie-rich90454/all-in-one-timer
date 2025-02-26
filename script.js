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
    setInterval(function(){
        if (flashing){
            cdBox.style.backgroundColor="#DE0000";
        }
        else{
            cdBox.style.backgroundColor="#F0F0F0";
        }
        flashing=!flashing;
    },500);
}
function parseHMS(seconds){
    let h=Math.floor(seconds/3600);
    let m=Math.floor((seconds%3600)/60);
    let s=seconds%60;
    return {h,m,seconds:s}
}
document.getElementById("timer-setter").addEventListener("click",function(){
    let totalSeconds=parseInt(document.getElementById("stopwatch-hour").valueAsNumber)*3600+parseInt(document.getElementById("stopwatch-minute").valueAsNumber)*60+parseInt(document.getElementById("stopwatch-seconds").valueAsNumber);
    updateCDDisplay();
    let prasedTime=parseHMS(totalSeconds);
    
    clearInterval(cdInterval);
    cdInterval=setInterval(countdown,1000);
});