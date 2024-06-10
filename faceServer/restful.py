from flask import Flask, request

app = Flask(__name__)

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return '파일이 없습니다', 400
    file = request.files['file']
    if file.filename == '':
        return '파일이 선택되지 않았습니다', 400
    # 파일을 저장하거나 처리하는 코드를 추가하세요
    file.save('/path/to/save/' + file.filename)
    return '파일 업로드 성공', 200

if __name__ == '__main__':
    app.run(host="0.0.0.0", port="5000")