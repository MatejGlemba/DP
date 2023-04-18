from utils.crypto import Crypto


listInput = []
input = "ahojky"
inputEnc = Crypto.encryptFun(input)
listInput.append(inputEnc)
input = "cauky"
inputEnc = Crypto.encryptFun(input)
listInput.append(inputEnc)

outputList = []
for l in listInput:
    outputList.append(Crypto.decryptFun(l))

for o in outputList:
    print(o)
