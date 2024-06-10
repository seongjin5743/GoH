from flask import Flask, render_template, Response, request, jsonify
import cv2
import pymysql
import camera as cam
import otp
import os
import serial
import time

app = Flask(__name__)

conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='root',
    passwd='1234',
    db='faceRecogition',
    charset='utf8'
)
cur = conn.cursor()

if os.path.isdir("./users") is False:
    os.mkdir('./users')

folder_path = './users/'
cnt = 0
back_server = 'http://223.194.139.86:8080'

find_user = False
user_id = ''
user_email = ''
user_time = 0
def gen_register_face():
    global user_id
    if user_id != '':
        os.mkdir('./users/' + user_id)

    capture = cv2.VideoCapture(0)
    capture.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    capture.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    while True:
        ref, frame = capture.read()
        global cnt
        if cnt == 50:
            print('촬영완료')
            capture.release()

        if not ref:
            break
        else:
            face_detect = cam.camera(frame, user_id, cnt)
            if face_detect:
                cnt += 1
            ref, buffer = cv2.imencode('.jpg', frame)
            frame = buffer.tobytes()
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')

def gen_recognize_face():
    capture = cv2.VideoCapture(0)
    capture.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    capture.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    global find_user
    find_user = False
    cnt_frame = 0
    while True:
        ref, frame = capture.read()
        if not ref:
            break
        else:
            frame, is_user = cam.face_recognition(frame, user_id)
            if is_user:
                if cnt_frame >= 15:
                    find_user = True
                    capture.release()
            ref, buffer = cv2.imencode('.jpg', frame)
            frame = buffer.tobytes()
            cnt_frame += 1
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')

@app.route('/')
def index():
    return render_template('login.html')

@app.route('/video_register')
def video_register():
    return Response(gen_register_face(), mimetype='multipart/x-mixed-replace; boundary=frame')

@app.route('/video_recognize')
def video_recognize():
    return Response(gen_recognize_face(), mimetype='multipart/x-mixed-replace; boundary=frame')

@app.route('/login', methods=["POST"])
def login():
    result = request.form

    sql = 'select * from userInfo where user_id = %s and user_pwd = %s'
    cur.execute(sql, (result.get("user_id"), result.get("user_pwd")))
    sql_result = cur.fetchall()
    print(sql_result)
    for i in sql_result:
        print(i)

    if sql_result == ():
        return "<script>alert('없는 사용자입니다');history.back(-1);</script>"
    else:
        global user_id
        global user_email
        user_id = result.get("user_id")
        user_email = sql_result[0][2]
        print(user_email)
        return render_template('faceRecognize.html')

@app.route('/signup')
def signup():
    return render_template('signup.html')

@app.route('/signupUser', methods=["post"])
def signupUser():
    result = request.form

    sql = 'select * from userInfo where user_id = %s'
    cur.execute(sql, (result.get("user_id")))
    sql_result = cur.fetchall()

    if sql_result == ():
        sql = 'insert into userInfo values (%s, %s, %s, %s)'

        cur.execute(sql, (result.get("user_id"), result.get("user_pwd"), result.get("user_email"), result.get("user_name")))
        conn.commit()
        global user_id
        user_id = result.get("user_id")
        return "<script>alert('회원가입이 완료되었습니다');location.href='/faceRegister';</script>"
    else:
        return "<script>alert('이미 존재하는 아이디입니다');history.back(-1);</script>"

@app.route('/faceRegister', methods=['POST', 'GET'])
def faceRegister():
    if request.method == 'POST':
        params = request.json
        print(params)
        global user_id
        user_id = params["memberId"]
    return render_template('faceRegister.html')

@app.route('/checkPictureCount', methods=['POST'])
def checkPictureCount():
    global cnt
    return jsonify({
        'count': cnt
    })

@app.route('/faceTrain')
def faceTrain():
    cam.train_face_recognitions(user_id)
    global cnt
    cnt = 0
    return f"<script>alert('얼굴 등록이 완료되었습니다');location.href='{back_server}';</script>"

@app.route('/faceRecognize', methods=['POST','GET'])
def faceRecognize():
    if request.method == 'POST':
        params = request.json
        print('faceRecognize')
        print(params)
        global user_id
        user_id = params["memberId"]
        global user_email
        user_email = params["memberEmail"]
    return render_template('faceRecognize.html')


@app.route('/checkRecognize', methods=['POST'])
def checkRecognize():
    global find_user
    return jsonify({
        'check': find_user
    })

@app.route('/receiveUserData', methods=['POST'])
def receiveUserData():
    params = request.json
    global user_id
    user_id = params["memberName"]
    return render_template('faceRegister.html')

@app.route('/otpSend')
def otpSend():
    global user_email
    global user_time
    user_time = otp.send_to_user(user_email)
    return f"<script>alert('{user_email}로 이메일을 전송했습니다.');location.href='/otpPage'</script>"

@app.route('/otpPage')
def otpPage():
    global user_email
    return render_template('otpPage.html', user_email=user_email)

@app.route('/otpCheck', methods = ['POST'])
def otpCheck():
    global user_email
    result = request.form
    check_correct = otp.check_otp_code(user_email, result.get("otp_code"), user_time)
    if check_correct == 2:
        return f"<script>alert('시간이 초과되었습니다');location.href='{back_server}';</script>"
    elif check_correct == 1:
        # 시리얼 추가
        arduino = serial.Serial(port='COM7', baudrate=9600, timeout=1)
        count = 0
        while True:
            if count < 10:
                arduino.write(b'1')
                time.sleep(1)
                count += 1
            else:
                arduino.write(b'0')
                time.sleep(1)
                return f"<script>alert('인증이 완료되었습니다');location.href='{back_server}';</script>"

    else:
        return f"<script>alert('잘못된 입력입니다');location.href='{back_server}';</script>"

@app.route('/main')
def main():
    return render_template('homepage.html')

if __name__ == "__main__":
    app.run(host="0.0.0.0", port="5000")
