from flask import Flask, request, jsonify
from flask_restful import Resource, Api, reqparse, abort
import json
from konlpy.tag import Okt
import requests
import re, math
from collections import Counter
from model_load import *

okt = Okt()
Question = None

# Flask 인스턴스 정리
app = Flask(__name__)
api = Api(app)


@app.route('/input', methods=['POST'])
#요정받은 사용자 질문을 모델로 전달하여 수행결과를 반환하는 함수
def Return_Answer():
    global Question
    global UserCode
    global department
    global REAL_Q
    global Return_A
    REAL_R = ""
    result = ""

    # 요청받은 사용자 질문 저장
    input = request.get_json();
    dump_data = json.dumps(input)
    json_data =json.loads(dump_data)

    Question = json_data['Senten']
    UserCode = json_data['code']
    department = json_data['depart']

    # Data Transformation
    CodeChange(UserCode)
    Question = josa(Question)
    filter_Q = filter(CODE, department, Question)
    REAL_Q = Stu_ID_Change(filter_Q)

    path1 = './[최종]data/[최종] 교양교과목안내.txt'
    path2 = './[최종]data/[최종] 인기강좌 강의매매방지 시스템 적용안내.txt'
    path3 = './[최종]data/[최종] 졸업이수학점.txt'
    
    p1 = cos_docs(path1, REAL_Q)
    p2 = cos_docs(path2, REAL_Q)
    p3 = cos_docs(path3, REAL_Q)

    #print(REAL_Q)

    # 3개 모델 중 유사도가 가장 높은 모델로 선정하여 모델 값을 반환함
    if p1 > p2 and p1 > p3:
        result = test_0(REAL_Q)
        #print("1")
    elif p2 > p1 and p2 > p3:
        result = test_1(REAL_Q)
        #print("2")
    elif p3 > p1 and p3 > p2:
        result = test_2(REAL_Q)
        #print("3")
    
    # 최종 답변만 출력하기 위해 필요없는 문자 삭제
    result_index = result.find("<")
    result = result[:result_index]
    REAL_R = re.sub('[-,#/\?:^$.@*\"※&%ㆍ!』\\‘|\(\)\[\]\<\>`\'…》]', '', result)

    #print(REAL_R)

    dict = {'Answer': REAL_R}
    
    # json 응답 반환
    return jsonify(dict)
    
#DB에 저장된 학번(숫자)를 **학번으로 변경하는 함수 
#사용자 질문 속 학번과 관련한 단어가 없다면 DB에 저장된 학번을 질문에 넣기 위해 생성함
def CodeChange(code):
    global CODE 
    if code >= 2020000000 :
        CODE = "20학번"
    elif 2019000000 <= code < 2020000000 :
        CODE = "19학번"
    
    elif 2018000000 <= code < 2019000000 :
        CODE = "18학번" 
    
    elif 2017000000 <= code < 2018000000 :
        CODE = "17학번"
    
    elif 2016000000 <= code < 2017000000 :
        CODE = "16학번"
        
    elif code < 2016000000 :
        CODE = "15학번"      

    return CODE     


#조사 및 띄어쓰기를 담당하는 함수
def josa(Q):
    linesplit = Q.split()
    seq = ''
    for i in linesplit:
        e = okt.pos(i)
        for j in e:
            if j[1] == 'Josa':
                e.remove(j)
        for k in e:
            seq = seq + k[0]
        else:
            seq = seq + ' '
    seq = seq + '\n'

    return seq


#(학번/학부) 단어 별 유무에 맞게 사용자 질문을 변경하는 함수
def filter(code, depart, Q):
    global Add_Q 
    global combine_Q

    if ('학번' in Q) and ('학부' in Q): 
        return Q

    elif ('학번' not in Q) and ('학부' in Q):
        Add_Q = " ".join([code])
        combine_Q = " ".join([Add_Q, Q])
        return combine_Q


    elif ('학번' in Q) and ('학부' not in Q):
        Add_Q = " ".join([depart])  
        combine_Q = " ".join([Add_Q, Q])
        return combine_Q

    elif ('학번' not in Q) and ('학부' not in Q):
        Add_Q = " ".join([code, depart]) 
        combine_Q = " ".join([Add_Q, Q])
        return combine_Q


#마지막으로 필터링된 사용자 질문 속 학번 중 15학번 미만일 경우 15학번으로 설정해주는 함수
def Stu_ID_Change(Q):
    code_index = Q.find("학번")
    ID_index = Q[code_index-2:code_index]

    if int(ID_index) < 15 :
        replace_Q = Q.replace(ID_index,"15")
    else: 
        replace_Q = Q

    return replace_Q

Word = re.compile(r'\w+')

# 두 vector 간의 cosine 유사도 반환 함수
def get_cosine(vec1, vec2):
     intersection = set(vec1.keys()) & set(vec2.keys())
     numerator = sum([vec1[x] * vec2[x] for x in intersection])

     sum1 = sum([vec1[x]**2 for x in vec1.keys()])
     sum2 = sum([vec2[x]**2 for x in vec2.keys()])
     denominator = math.sqrt(sum1) * math.sqrt(sum2)

     if not denominator:
        return 0.0
     else:
        return float(numerator) / denominator

# text를 vector로 변환하는 함수
def text_to_vector(text):
     words = Word.findall(text)
     return Counter(words)

# 문서와 text와의 cosine 유사도를 계산하는 함수
def cos_docs(path, text):
    f = open(path, 'r', encoding = 'utf8')
    flist = f.readlines()

    sum_i = 0

    for i in flist:
        vector1 = text_to_vector(i)
        vector2 = text_to_vector(text)

        cosine = get_cosine(vector1, vector2)
        sum_i += cosine

    return sum_i / len(flist)


if __name__ == '__main__':
    # 사용자가 원하는 URL과 Port 입력
    app.run(host="User_URL", port="User_Port", debug=True)
    
   

   
                                     