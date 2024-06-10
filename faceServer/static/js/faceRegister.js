const countPicture = setInterval(()=>{
    $.ajax({
        url: '/checkPictureCount',
        type: 'post',
        success: function(response) {
            console.log(response);
            if(parseInt(response["count"])>=50) {
                clearInterval(countPicture)
                location.href='/faceTrain';
            }
        },
        error: function(error) {
            console.log(error);
        }
    })
}, 1000);