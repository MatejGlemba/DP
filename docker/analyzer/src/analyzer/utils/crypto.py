from Crypto.Cipher import AES
from Crypto.Util.Padding import pad, unpad

key = 'totojehashklucok'.encode('utf-8')
iv = 'totojeinitvector'.encode('utf-8')

class Crypto:
    
    def encryptFun(data: str):
        cipher = AES.new(key, AES.MODE_CBC, iv=iv)
        plaintext = data.encode('utf-8')
        padded_plaintext = pad(plaintext, AES.block_size)
        ciphertext = cipher.encrypt(padded_plaintext)
        return ciphertext

    def decryptFun(ciphertext: bytes):
        cipher = AES.new(key, AES.MODE_CBC, iv=iv)
        decrypted_padded_plaintext = cipher.decrypt(ciphertext)
        decrypted_plaintext = unpad(decrypted_padded_plaintext, AES.block_size)
        return decrypted_plaintext.decode('utf-8')
        

