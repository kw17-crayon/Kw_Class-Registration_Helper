import copy
import random
import torch
import torch.nn as nn
from torch.autograd import Variable
import torch.nn.functional as F
import torch.optim as optim
flatten = lambda l: [item for sublist in l for item in sublist]

USE_CUDA = torch.cuda.is_available()
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

FloatTensor = torch.FloatTensor
LongTensor = torch.LongTensor
ByteTensor = torch.ByteTensor

# 문서의 단어들 voca 저장한 dict변수
word2index = {}
index2word = {}

# 모델 
class DMN(nn.Module):
    def __init__(self, input_size, hidden_size, output_size, dropout_p=0.1):
        super(DMN, self).__init__()
        
        self.hidden_size = hidden_size
        self.embed = nn.Embedding(input_size, hidden_size, padding_idx=0) #sparse=True)
        self.input_gru = nn.GRU(hidden_size, hidden_size, batch_first=True)
        self.question_gru = nn.GRU(hidden_size, hidden_size, batch_first=True)
        
        self.gate = nn.Sequential(
                            nn.Linear(hidden_size * 4, hidden_size),
                            nn.Tanh(),
                            nn.Linear(hidden_size, 1),
                            nn.Sigmoid()
                        )
        
        self.attention_grucell =  nn.GRUCell(hidden_size, hidden_size)
        self.memory_grucell = nn.GRUCell(hidden_size, hidden_size)
        self.answer_grucell = nn.GRUCell(hidden_size * 2, hidden_size)
        self.answer_fc = nn.Linear(hidden_size, output_size)
        
        self.dropout = nn.Dropout(dropout_p)
        
    def init_hidden(self, inputs):
        hidden = Variable(torch.zeros(1, inputs.size(0), self.hidden_size))
        return hidden.cuda() if USE_CUDA else hidden
    
    def init_weight(self):
        nn.init.xavier_uniform(self.embed.state_dict()['weight'])
        
        for name, param in self.input_gru.state_dict().items():
            if 'weight' in name: nn.init.xavier_normal(param)
        for name, param in self.question_gru.state_dict().items():
            if 'weight' in name: nn.init.xavier_normal(param)
        for name, param in self.gate.state_dict().items():
            if 'weight' in name: nn.init.xavier_normal(param)
        for name, param in self.attention_grucell.state_dict().items():
            if 'weight' in name: nn.init.xavier_normal(param)
        for name, param in self.memory_grucell.state_dict().items():
            if 'weight' in name: nn.init.xavier_normal(param)
        for name, param in self.answer_grucell.state_dict().items():
            if 'weight' in name: nn.init.xavier_normal(param)
        
        nn.init.xavier_normal(self.answer_fc.state_dict()['weight'])
        self.answer_fc.bias.data.fill_(0)
        
    def forward(self, facts, fact_masks, questions, question_masks, num_decode, episodes=3, is_training=False):
        """
        facts : (B,T_C,T_I) / LongTensor in List # batch_size, num_of_facts, length_of_each_fact(padded)
        fact_masks : (B,T_C,T_I) / ByteTensor in List # batch_size, num_of_facts, length_of_each_fact(padded)
        questions : (B,T_Q) / LongTensor # batch_size, question_length
        question_masks : (B,T_Q) / ByteTensor # batch_size, question_length
        """
        # Input Module
        C = [] # encoded facts
        for fact, fact_mask in zip(facts, fact_masks):
            embeds = self.embed(fact)
            if is_training:
                embeds = self.dropout(embeds)
            hidden = self.init_hidden(fact)
            outputs, hidden = self.input_gru(embeds, hidden)
            real_hidden = []

            for i, o in enumerate(outputs): # B,T,D
                real_length = fact_mask[i].data.tolist().count(0) 
                real_hidden.append(o[real_length - 1])

            C.append(torch.cat(real_hidden).view(fact.size(0), -1).unsqueeze(0))
        
        encoded_facts = torch.cat(C) # B,T_C,D
        
        # Question Module
        embeds = self.embed(questions)
        if is_training:
            embeds = self.dropout(embeds)
        hidden = self.init_hidden(questions)
        outputs, hidden = self.question_gru(embeds, hidden)
        
        if isinstance(question_masks, torch.autograd.Variable):
            real_question = []
            for i, o in enumerate(outputs): # B,T,D
                real_length = question_masks[i].data.tolist().count(0) 
                real_question.append(o[real_length - 1])
            encoded_question = torch.cat(real_question).view(questions.size(0), -1) # B,D
        else: # for inference mode
            encoded_question = hidden.squeeze(0) # B,D
            
        # Episodic Memory Module
        memory = encoded_question
        T_C = encoded_facts.size(1)
        B = encoded_facts.size(0)
        for i in range(episodes):
            hidden = self.init_hidden(encoded_facts.transpose(0, 1)[0]).squeeze(0) # B,D
            for t in range(T_C):
                #TODO: fact masking
                #TODO: gate function => softmax
                z = torch.cat([
                                    encoded_facts.transpose(0, 1)[t] * encoded_question, # B,D , element-wise product
                                    encoded_facts.transpose(0, 1)[t] * memory, # B,D , element-wise product
                                    torch.abs(encoded_facts.transpose(0,1)[t] - encoded_question), # B,D
                                    torch.abs(encoded_facts.transpose(0,1)[t] - memory) # B,D
                                ], 1)
                g_t = self.gate(z) # B,1 scalar
                hidden = g_t * self.attention_grucell(encoded_facts.transpose(0, 1)[t], hidden) + (1 - g_t) * hidden
                
            e = hidden
            memory = self.memory_grucell(e, memory)
        
        # Answer Module
        answer_hidden = memory
        start_decode = Variable(LongTensor([[word2index['<s>']] * memory.size(0)])).transpose(0, 1)
        y_t_1 = self.embed(start_decode).squeeze(1) # B,D
        
        decodes = []
        for t in range(num_decode):
            answer_hidden = self.answer_grucell(torch.cat([y_t_1, encoded_question], 1), answer_hidden)
            decodes.append(F.log_softmax(self.answer_fc(answer_hidden),1))
        return torch.cat(decodes, 1).view(B * num_decode, -1)
    
def bAbI_data_load(path):
    try:
        f = open(path, 'r', encoding='utf-8')
        data = f.readlines()
    except:
        print("Such a file does not exist at %s".format(path))
        return None
    
    data = [d[:-1] for d in data]
    data_p = []
    fact = []
    qa = []
    try:
        for d in data:
            index = d.split(' ')[0]
            if index == '1':
                fact = []
                qa = []
            if '?' in d:
                temp = d.split('\t')
                q = temp[0].strip().replace('?', '').split(' ')[1:] + ['?']
                a = temp[1].split() + ['</s>']
                stemp = copy.deepcopy(fact)
                data_p.append([stemp, q, a])
            else:
                tokens = d.replace('.', '').split(' ')[1:] + ['</s>']
                fact.append(tokens)
    except Exception as e:
        print(e)
        print("Please check the data is right")
        return None
    return data_p

def pad_to_fact(fact, x_to_ix): # this is for inference
    
    max_x = max([s.size(1) for s in fact])
    x_p = []
    for i in range(len(fact)):
        if fact[i].size(1) < max_x:
            x_p.append(torch.cat([fact[i], Variable(LongTensor([x_to_ix['<PAD>']] * (max_x - fact[i].size(1)))).view(1, -1)], 1))
        else:
            x_p.append(fact[i])
        
    fact = torch.cat(x_p)
    fact_mask = torch.cat([Variable(ByteTensor(tuple(map(lambda s: s ==0, t.data))), volatile=False) for t in fact]).view(fact.size(0), -1)
    return fact, fact_mask

def prepare_sequence(seq, to_index):
    idxs = list(map(lambda w: to_index[w] if to_index.get(w) is not None else to_index["<UNK>"], seq))
    return Variable(LongTensor(idxs))

# test_0 : 교양교과목안내
# test_1 : 인기 강좌 매매 방지
# test_2 : 졸업이수학점 안내
def test_0(seq):
    global word2index
    global index2word

    test_data = bAbI_data_load('./[최종]data/[최종] 교양교과목안내.txt')
    
    fact,q,a = list(zip(*test_data))
    
    vocab = list(set(flatten(flatten(fact)) + flatten(q) + flatten(a)))
    
    word2index={'<PAD>': 0, '<UNK>': 1, '<s>': 2, '</s>': 3}
    for vo in vocab:
        if word2index.get(vo) is None:
            word2index[vo] = len(word2index)
    index2word = {v:k for k, v in word2index.items()}
    
    for t in test_data:
        for i, fact in enumerate(t[0]):
            t[0][i] = prepare_sequence(fact, word2index).view(1, -1)
    
        t[1] = prepare_sequence(t[1], word2index).view(1, -1)
        t[2] = prepare_sequence(t[2], word2index).view(1, -1)
    
    # 저장된 모델을 load
    model = DMN(len(word2index), 80, len(word2index))
    #model.hidden = model.init_hidden()
    loss_function = nn.CrossEntropyLoss(ignore_index=0)
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    checkpoint = torch.load('./[최종]model/[교양교과목0603]crayon.pth')
    model.load_state_dict(checkpoint['model_state_dict'])
    optimizer.load_state_dict(checkpoint['optimizer_state_dict'])   
    loss_function.state_dict(checkpoint['criterion_state_dict'])
    epoch = checkpoint['epoch']
    loss = checkpoint['loss']

    model.to("cpu")
    model.eval()

    t = random.choice(test_data)
    fact, fact_mask = pad_to_fact(t[0], word2index)
    #question = t[1]
    #question_mask = Variable(ByteTensor([0] * t[1].size(1)), volatile=False).unsqueeze(0)
    #answer = t[2].squeeze(0)
    
    # 사용자의 입력 seq를 tensor로 변환하여
    # model에 넣어 pred값 return
    s = seq
    sl = []
    sl_mask = []
    for i in s.split():
        try:
            sl.append(word2index[i])
            sl_mask.append(0)   
        except:
            sl.append(1)
            sl_mask.append(0)
            pass

    sl = torch.tensor([sl])
    sl_mask = torch.tensor([sl_mask])

    print(sl, sl_mask)
    
    pred = model([fact], [fact_mask], sl, sl_mask, 10, 3, True)
    
    ws = ''
    for i in pred.max(1)[1].data.tolist():
        w = index2word[i]
        ws = ws + w + ' '
    
    return ws

def test_1(seq):
    global word2index
    global index2word
    
    test_data = bAbI_data_load('./[최종]data/[최종] 인기강좌 강의매매방지 시스템 적용안내.txt')
    
    fact,q,a = list(zip(*test_data))
    vocab = list(set(flatten(flatten(fact)) + flatten(q) + flatten(a)))
    
    word2index={'<PAD>': 0, '<UNK>': 1, '<s>': 2, '</s>': 3}
    for vo in vocab:
        if word2index.get(vo) is None:
            word2index[vo] = len(word2index)
    index2word = {v:k for k, v in word2index.items()}
    
    for t in test_data:
        for i, fact in enumerate(t[0]):
            t[0][i] = prepare_sequence(fact, word2index).view(1, -1)
    
        t[1] = prepare_sequence(t[1], word2index).view(1, -1)
        t[2] = prepare_sequence(t[2], word2index).view(1, -1)
    
    model = DMN(len(word2index), 80, len(word2index))
    #model.hidden = model.init_hidden()
    loss_function = nn.CrossEntropyLoss(ignore_index=0)
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    checkpoint = torch.load('./[최종]model/[인기강좌0603]crayon.pth')
    model.load_state_dict(checkpoint['model_state_dict'])
    optimizer.load_state_dict(checkpoint['optimizer_state_dict'])   
    loss_function.state_dict(checkpoint['criterion_state_dict'])
    epoch = checkpoint['epoch']
    loss = checkpoint['loss']

    model.to("cpu")
    model.eval()
    t = random.choice(test_data)
    fact, fact_mask = pad_to_fact(t[0], word2index)
    #question = t[1]
    #question_mask = Variable(ByteTensor([0] * t[1].size(1)), volatile=False).unsqueeze(0)
    #answer = t[2].squeeze(0)
    
    s = seq
    sl = []
    sl_mask = []
    for i in s.split():
        try:
            sl.append(word2index[i])
            sl_mask.append(0)   
        except:
            sl.append(1)
            sl_mask.append(0)
            #print("error")
            pass
    

    sl = torch.tensor([sl])
    sl_mask = torch.tensor([sl_mask])
    
    print("sl sl-mask : ", sl, sl_mask)
    
    pred = model([fact], [fact_mask], sl, sl_mask, 10, 3, True)
    
    ws = ''
    for i in pred.max(1)[1].data.tolist():
        w = index2word[i]
        ws = ws + w + ' '
    
    return ws

def test_2(seq):
    global word2index
    global index2word
    
    test_data = bAbI_data_load('./[최종]data/[최종] 졸업이수학점.txt')
    
    fact,q,a = list(zip(*test_data))
    vocab = list(set(flatten(flatten(fact)) + flatten(q) + flatten(a)))
    
    word2index={'<PAD>': 0, '<UNK>': 1, '<s>': 2, '</s>': 3}
    for vo in vocab:
        if word2index.get(vo) is None:
            word2index[vo] = len(word2index)
    index2word = {v:k for k, v in word2index.items()}
    print(word2index)

    for t in test_data:
        for i, fact in enumerate(t[0]):
            t[0][i] = prepare_sequence(fact, word2index).view(1, -1)
    
        t[1] = prepare_sequence(t[1], word2index).view(1, -1)
        t[2] = prepare_sequence(t[2], word2index).view(1, -1)
    
    model = DMN(len(word2index), 80, len(word2index))
    #model.hidden = model.init_hidden()
    loss_function = nn.CrossEntropyLoss(ignore_index=0)
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    checkpoint = torch.load('./[최종]model/[졸업이수학점0603]crayon.pth')
    model.load_state_dict(checkpoint['model_state_dict'])
    optimizer.load_state_dict(checkpoint['optimizer_state_dict'])   
    loss_function.state_dict(checkpoint['criterion_state_dict'])
    epoch = checkpoint['epoch']
    loss = checkpoint['loss']

    model.to("cpu")
    model.eval()
    t = random.choice(test_data)
    fact, fact_mask = pad_to_fact(t[0], word2index)
    #question = t[1]
    #question_mask = Variable(ByteTensor([0] * t[1].size(1)), volatile=False).unsqueeze(0)
    #answer = t[2].squeeze(0)
    
    s = seq
    sl = []
    sl_mask = []
    for i in s.split():
        try:
            sl.append(word2index[i])
            sl_mask.append(0)   
        except:
            sl.append(1)
            sl_mask.append(0)
            pass
    

    sl = torch.tensor([sl])
    sl_mask = torch.tensor([sl_mask])
    
    print(sl, sl_mask)
    
    pred = model([fact], [fact_mask], sl, sl_mask, 10, 3, True)
    
    ws = ''
    for i in pred.max(1)[1].data.tolist():
        w = index2word[i]
        ws = ws + w + ' '
    
    return ws
