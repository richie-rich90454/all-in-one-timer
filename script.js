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
