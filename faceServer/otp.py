import hashlib
import time
import math
import smtplib
from email.message import EmailMessage


def send_mail(receiver, header, content):
    # STMP 서버의 url과 port 번호
    smtp_server = 'smtp.naver.com'
    smtp_port = 465

    # 1. SMTP 서버 연결
    smtp = smtplib.SMTP_SSL(smtp_server, smtp_port)

    # 메일 송신자 이메일 / 비밀번호
    sender = 'junjun32918@naver.com'
    sender_pwd = 'lock!~9^9%?'

    # 2. SMTP 서버에 로그인
    smtp.login(sender, sender_pwd)

    # 3. MIME 형태의 이메일 메세지 작성
    message = EmailMessage()
    # 내용
    message.set_content(content)
    # 제목
    message["Subject"] = header
    # 보내는 사람의 이메일
    message["From"] = sender
    # 받는 사람 이메일
    message["To"] = receiver

    # 4. 서버로 메일 보내기
    smtp.send_message(message)

    # 5. 메일을 보내면 서버와의 연결 끊기
    smtp.quit()


def send_to_user(user_email):
    # 현재 시간 정보(초 정보 제거)
    now = time
    system_time = math.floor(now.time())

    # ID와 시간값을 합쳐서 sha256함수를 이용해서 해쉬값 생성
    before_hash = str(user_email)+str(system_time)
    hashed_data = hashlib.sha256(before_hash.encode()).hexdigest()

    header = "인증 코드"
    content = "코드번호 : "+hashed_data[0:6]+"을 입력해주세요"

    send_mail(user_email, header, content)
    return system_time


def check_otp_code(user_email, user_code, get_time):
    # 현재 시간 정보
    now = time
    system_time = math.floor(now.time())

    # 3분 지나면 코드 사용 불가
    if int(system_time) >= int(get_time)+180:
        return 2
    # 유저 이메일과 시간 정보를 이용한 otp 생성
    before_hash = str(user_email) + str(get_time)
    hashed_data = hashlib.sha256(before_hash.encode()).hexdigest()

    otp_code = hashed_data[0:6]

    # 올바른 코드 입력시 True 리턴
    if user_code == otp_code:
        return 1
    else:
        return 0
