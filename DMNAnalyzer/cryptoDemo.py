from utils.crypto import Crypto


input = "ahojky"
inputEnc = Crypto.encryptFun(input)
decr = Crypto.decryptFun(inputEnc)
print(decr)
