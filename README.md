# duofei_signaling-server
基于webRtc 的视频聊天，包含前后端代码
---
### 1.一对一聊天
   启动信令服务器，修改前端代码中的websockets地址（onetoone.js文件中）
1. 客户A访问地址 ../onetoone.html?token=nobodyknowme&name=clientA&remote=clientB，点击start
2. 客户B访问地址 ../onetoone.html?token=nobodyknowme&name=clientB&remote=clientA，点击start后，点击Call
### 2.多人视频聊天
   启动信令服务器，修改前端代码中的websockets地址（moretomore.js文件中），部署启动前端代码
1. 客户端A 访问地址 ../moretomore.html?name=clientA&roomId=001, 点击start
2. 客户端B 访问地址 ../moretomore.html?name=clientB&roomId=001,点击start
3. 客户端C 访问地址 ../moretomore.html?name=clientC&roomId=001,点击start
4. 客户端D 访问地址 ../moretomore.html?name=clientD&roomId=001,点击start, 并点击Call。
5. 整个完整的流程如上。 

**注：以上流程能够得到保证，其它流程我的代码均未考虑。并且执行一次以上流程可能需要重启信令服务器，重新执行整个流程。**