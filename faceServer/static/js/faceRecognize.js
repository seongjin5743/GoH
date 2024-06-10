const checkFace = setInterval(()=>{
    $.ajax({
        url: '/checkRecognize',
        type: 'post',
        success: function(response) {
            console.log(response);
            if(response["check"]==true) {
                clearInterval(checkFace)
                alert("얼굴이 확인되었습니다");
                location.href='/otpSend';
            }
        },
        error: function(error) {
            console.log(error);
        }
    })
}, 1000);