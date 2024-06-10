import cv2
import os
import numpy as np
from PIL import Image

if os.path.isdir("./users") is False:
    os.mkdir('./users')

face_classifier = cv2.CascadeClassifier('haarcascade_frontalface_default.xml')
face_recognizer = cv2.face.LBPHFaceRecognizer_create()

width, height = 220, 220
font = cv2.FONT_HERSHEY_COMPLEX_SMALL

def face_extractor(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    faces = face_classifier.detectMultiScale(gray, scaleFactor=1.3, minSize=(100, 100))
    if faces is ():
        return None

    # 얼굴을 찾으면 잘라서 리턴
    for (x, y, w, h) in faces:
        cropped_face = img[y:y + h, x:x + w]

    return cropped_face


def camera(frame, user, img_num):
    face = frame
    if face_extractor(frame) is None:
        return False
    face = cv2.resize(face_extractor(frame), (200, 200))
    face = cv2.cvtColor(face, cv2.COLOR_BGR2GRAY)
    path = './users/' + user + '/' + user + '_' + str(img_num) + '.jpg'
    cv2.imwrite(path, face)
    return True


def train_face_recognitions(user):
    path = './users/'+user
    faces = []
    ids = []

    paths = [os.path.join(path, f) for f in os.listdir(path)]
    for path in paths:
        image = Image.open(path).convert('L')
        image_np = np.array(image, 'uint8')
        ids.append(0)
        faces.append(image_np)

    ids = np.array(ids)
    lbph_classifier = cv2.face.LBPHFaceRecognizer_create(radius=1, neighbors=8, grid_x=8, grid_y=8)
    lbph_classifier.train(faces, ids)
    lbph_classifier.write('./classifier/lbph_classifier_'+user+'.yml')


def face_recognition(frame, user):
    is_user = False
    face_recognizer.read("./classifier/lbph_classifier_"+user+".yml")

    image_gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    detections = face_classifier.detectMultiScale(image_gray, scaleFactor=1.3, minNeighbors=5, minSize=(100, 100))

    for (x, y, w, h) in detections:
        image_face = cv2.resize(image_gray[y:y + w, x:x + h], (width, height))
        cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 0, 255), 2)
        id, confidence = face_recognizer.predict(image_face)
        name = user

        if id == 0:
            is_user = True
            cv2.putText(frame, name, (x, y + (w + 30)), font, 2, (0, 0, 255))

    return frame, is_user

