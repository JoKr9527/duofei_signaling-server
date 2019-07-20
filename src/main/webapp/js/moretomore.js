/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree.
 */

'use strict';

const startButton = document.getElementById('startButton');
const callButton = document.getElementById('callButton');
const hangupButton = document.getElementById('hangupButton');
callButton.disabled = true;
hangupButton.disabled = true;
startButton.addEventListener('click', start);
callButton.addEventListener('click', call);
hangupButton.addEventListener('click', hangup);

let startTime;
let user;
const localVideo = document.getElementById('localVideo');
//const remoteVideo = document.getElementById('remoteVideo');
const requestType = {
    USER_IDENTITY: 'USER_IDENTITY',
    SDP_OFFER: 'SDP_OFFER',
    SDP_ANSWER: 'SDP_ANSWER',
    SDP_CANDIDATE: 'SDP_CANDIDATE',
    CLIENT_MULTIPLY_CONNECTION_CREATEQUE: 'CLIENT_MULTIPLY_CONNECTION_CREATEQUE',
    SERVER_CALLEESRESP: 'SERVER_CALLEESRESP'
};

localVideo.addEventListener('loadedmetadata', function () {
    console.log(`Local video videoWidth: ${this.videoWidth}px,  videoHeight: ${this.videoHeight}px`);
});

/*remoteVideo.addEventListener('loadedmetadata', function () {
    console.log(`Remote video videoWidth: ${this.videoWidth}px,  videoHeight: ${this.videoHeight}px`);
});

remoteVideo.addEventListener('resize', () => {
    console.log(`Remote video size changed to ${remoteVideo.videoWidth}x${remoteVideo.videoHeight}`);
    // We'll use the first onsize callback as an indication that video has started
    // playing out.
    if (startTime) {
        const elapsedTime = window.performance.now() - startTime;
        console.log('Setup time: ' + elapsedTime.toFixed(3) + 'ms');
        startTime = null;
    }
});*/

let localStream;
let pcs = [];
let ws;
const offerOptions = {
    offerToReceiveAudio: 1,
    offerToReceiveVideo: 1
};

// 建立连接
connect();


function getName(pc) {
    return (pc === pc1) ? 'pc1' : 'pc2';
}

function getOtherPc(pc) {
    return (pc === pc1) ? pc2 : pc1;
}

async function start() {
    console.log('Requesting local stream');
    startButton.disabled = true;
    try {
        const stream = await navigator.mediaDevices.getUserMedia({audio: true, video: true});
        console.log('Received local stream');
        localVideo.srcObject = stream;
        localStream = stream;
        callButton.disabled = false;
    } catch (e) {
        alert(`getUserMedia() error: ${e.name}`);
    }
}

function getSelectedSdpSemantics() {
    const sdpSemanticsSelect = document.querySelector('#sdpSemantics');
    const option = sdpSemanticsSelect.options[sdpSemanticsSelect.selectedIndex];
    return option.value === '' ? {} : {sdpSemantics: option.value};
}

async function call() {
    callButton.disabled = true;
    hangupButton.disabled = false;
    console.log('Starting call');
    console.log("发送创建多个请求消息...");
    ws.send(JSON.stringify({
        msgType: requestType.CLIENT_MULTIPLY_CONNECTION_CREATEQUE,
        msgBody: user
    }));
    /*startTime = window.performance.now();
    const videoTracks = localStream.getVideoTracks();
    const audioTracks = localStream.getAudioTracks();
    if (videoTracks.length > 0) {
        console.log(`Using video device: ${videoTracks[0].label}`);
    }
    if (audioTracks.length > 0) {
        console.log(`Using audio device: ${audioTracks[0].label}`);
    }
    const configuration = getSelectedSdpSemantics();
    console.log('RTCPeerConnection configuration:', configuration);
    pc1 = new RTCPeerConnection(configuration);
    console.log('Created local peer connection object pc1');
    pc1.addEventListener('icecandidate', e => onIceCandidate(e));
    //pc2 = new RTCPeerConnection(configuration);
    //console.log('Created remote peer connection object pc2');
    //pc2.addEventListener('icecandidate', e => onIceCandidate(pc2, e));
    pc1.addEventListener('iceconnectionstatechange', e => onIceStateChange(pc1, e));
    //pc2.addEventListener('iceconnectionstatechange', e => onIceStateChange(pc2, e));
    //pc2.addEventListener('track', gotRemoteStream);
    pc1.addEventListener('track', gotRemoteStream);

    localStream.getTracks().forEach(track => pc1.addTrack(track, localStream));
    console.log('Added local stream to pc1');

    try {
        console.log('pc1 createOffer start');
        const offer = await pc1.createOffer(offerOptions);
        await onCreateOfferSuccess(offer);
    } catch (e) {
        onCreateSessionDescriptionError(e);
    }*/
}

function onCreateSessionDescriptionError(error) {
    console.log(`Failed to create session description: ${error.toString()}`);
}

async function onCreateOfferSuccess(desc) {
    ws.send(JSON.stringify(desc));
    console.log(`Offer from pc1\n${desc.sdp}`);
    console.log('pc1 setLocalDescription start');
    try {
        await pc1.setLocalDescription(desc);
        onSetLocalSuccess(pc1);
    } catch (e) {
        onSetSessionDescriptionError();
    }

    /*console.log('pc2 setRemoteDescription start');
    try {
        await pc2.setRemoteDescription(desc);
        onSetRemoteSuccess(pc2);
    } catch (e) {
        onSetSessionDescriptionError();
    }

    console.log('pc2 createAnswer start');
    // Since the 'remote' side has no media stream we need
    // to pass in the right constraints in order for it to
    // accept the incoming offer of audio and video.
    try {
        const answer = await pc2.createAnswer();
        await onCreateAnswerSuccess(answer);
    } catch (e) {
        onCreateSessionDescriptionError(e);
    }*/
}

function onSetLocalSuccess(pc) {
    console.log(`${getName(pc)} setLocalDescription complete`);
}

function onSetRemoteSuccess(pc) {
    console.log(`${getName(pc)} setRemoteDescription complete`);
}

function onSetSessionDescriptionError(error) {
    console.log(`Failed to set session description: ${error.toString()}`);
}

function gotRemoteStream(e) {
    if (remoteVideo.srcObject !== e.streams[0]) {
        remoteVideo.srcObject = e.streams[0];
        console.log('pc received remote stream');
    }
}

async function onCreateAnswerSuccess(desc) {
    console.log(`Answer from pc2:\n${desc.sdp}`);
    console.log('pc2 setLocalDescription start');
    try {
        ws.send(JSON.stringify(desc));
        await pc2.setLocalDescription(desc);
        onSetLocalSuccess(pc2);
    } catch (e) {
        onSetSessionDescriptionError(e);
    }
}

async function onIceCandidate(event,username) {
    try {
        console.log(username+">>> event.candidate..." + event.candidate);
        ws.send(JSON.stringify(
            {
                msgType: requestType.SDP_CANDIDATE,
                msgBody: {
                    userName: username,
                    sdp: event.candidate
                }
            }
        ));
        console.log(username+">>> send candidate msg success");
    } catch (e) {
        onAddIceCandidateError(pc, e);
    }
}

function onAddIceCandidateSuccess(pc) {
    console.log(`${pc} addIceCandidate success`);
}

function onAddIceCandidateError(pc, error) {
    console.log(`${pc} failed to add ICE Candidate: ${error.toString()}`);
}

function onIceStateChange(pc, event) {
    if (pc) {
        console.log(`${pc} ICE state: ${pc.iceConnectionState}`);
        console.log('ICE state change event: ', event);
    }
}

function hangup() {
    console.log('Ending call');
    pcs.forEach(function (pcc) {
        pcc.pc.close();
    });
    pcs = {};
    hangupButton.disabled = true;
    callButton.disabled = false;
}

//websocket 连接建立
function connect() {
    ws = new WebSocket("ws://192.168.3.72:8083/signaling/complexSdpExchange");
    ws.onopen = function () {
        console.log("connect success ... ");
        // 初始化当前用户身份
        let urlParams = urlSearch();
        user = {
            name: urlParams['name'],
            roomId: urlParams['roomId']
        };
        console.log("current user...", user);
        ws.send(JSON.stringify({
            msgType: requestType.USER_IDENTITY,
            msgBody: user
        }));
    };
    ws.onclose = function () {
        console.log("connect close...");
    };
    ws.onmessage = function (evt) {
        console.log("receive ... " + evt.data);
        let result = evt.data;

        result = JSON.parse(result);
        const msgBody = result.msgBody;
        // 接收到成员消息响应，创建多个连接
        if(result.msgType === requestType.SERVER_CALLEESRESP){
            console.log("接收到创建多个连接消息响应" + result.msgBody);
            createConnections(result.msgBody).catch((error)=>{
                console.log("创建成员连接出错..." , result.msgBody,error);
            });
        }
        // 接收到offer消息时
        if(result.msgType === requestType.SDP_OFFER){
            console.log("接收到offer消息" , result.msgBody);
            // 创建
            const rtcPeerConnection = createRTCPeerConnection(result.msgBody.userName, createRemoteVideo(result.msgBody.userName));
            rtcPeerConnection.setRemoteDescription(JSON.parse(msgBody.sdp)).catch((error)=>{
                console.log(result.msgBody.userName + ">>> setRemoteDesc error" , msgBody.sdp,error);
            });
            // 应答
            rtcPeerConnection.createAnswer().catch((error)=>{
                console.log(result.msgBody.userName + ">>> createAnswer error" , error);
            }).then(answer=>{
                rtcPeerConnection.setLocalDescription(answer).catch(()=>{
                    console.log(result.msgBody.userName + ">>> setLocalDesc error" , answer);
                });
                // 响应answer
                ws.send(JSON.stringify({
                    msgType: requestType.SDP_ANSWER,
                    msgBody: {
                        userName:msgBody.userName,
                        sdp: answer
                    }
                }));
            });
            // 发送创建多个请求消息
            console.log("发送创建多个请求消息...");
            ws.send(JSON.stringify({
                msgType: requestType.CLIENT_MULTIPLY_CONNECTION_CREATEQUE,
                msgBody: user
            }));
        }
        // 接收到answer消息时
        if(result.msgType === requestType.SDP_ANSWER){
            console.log("接收到answer消息...");
            pcs.forEach(function (pcc) {
               if(pcc.userName === msgBody.userName){
                   pcc.pc.setRemoteDescription(JSON.parse(msgBody.sdp)).catch(()=>{
                       console.log(msgBody.userName + ">>> setRemoteDesc error" + msgBody.sdp);
                   });
               }
            });
        }
        // 接收到candidate消息时
        if(result.msgType === requestType.SDP_CANDIDATE){
            console.log("接收到candidate消息...");
            pcs.forEach(function (pcc) {
                console.log(pcc.userName + "----" + msgBody.userName);
                if(pcc.userName === msgBody.userName){
                    pcc.pc.addIceCandidate(JSON.parse(msgBody.sdp)).catch((error)=>{
                        console.log(msgBody.userName + ">>> addIceCandidate error" , msgBody.sdp,error);
                    });
                    console.log(`${pcc.pc} addIceCandidate success`);
                }
            });
        }
    };
}

//url 参数获取
function urlSearch() {
    var name, value;
    let result = [];
    var str = location.href; //取得整个地址栏
    var num = str.indexOf("?");
    str = str.substr(num + 1); //取得所有参数   stringvar.substr(start [, length ]

    var arr = str.split("&"); //各个参数放到数组里
    console.log(arr);
    for (var i = 0; i < arr.length; i++) {
        num = arr[i].indexOf("=");
        if (num > 0) {
            name = arr[i].substring(0, num);
            value = arr[i].substr(num + 1);
            result[name] = value;
        }
    }
    return result;
}

// 被动建立连接
async function called(desc) {
    const configuration = getSelectedSdpSemantics();
    pc2 = new RTCPeerConnection(configuration);
    console.log('Created  peer connection object pc2');
    pc2.addEventListener('icecandidate', e => onIceCandidate(e));
    pc2.addEventListener('iceconnectionstatechange', e => onIceStateChange(pc2, e));
    pc2.addEventListener('track', gotRemoteStream);
    localStream.getTracks().forEach(track => pc2.addTrack(track, localStream));
    console.log('pc2 setRemoteDescription start');
    try {
        await pc2.setRemoteDescription(desc);
        onSetRemoteSuccess(pc2);
    } catch (e) {
        onSetSessionDescriptionError(e);
    }

    console.log('pc2 createAnswer start');
    // Since the 'remote' side has no media stream we need
    // to pass in the right constraints in order for it to
    // accept the incoming offer of audio and video.
    try {
        const answer = await pc2.createAnswer();
        await onCreateAnswerSuccess(answer);
    } catch (e) {
        onCreateSessionDescriptionError(e);
    }
}

// 创建多个点对点连接和video元素
async function createConnections(userNames) {
    const tempPcs = [];
    userNames.forEach(function (username){
        tempPcs.push({
            userName: username,
            pc: createRTCPeerConnection(username,createRemoteVideo(username))
        });
    });
    // 发送多个用户的username和offer
    const offers = [];
    for (let i = 0; i < tempPcs.length; i++) {
        const pcc = tempPcs[i];
        const offer = await pcc.pc.createOffer(offerOptions);
        pcc.pc.setLocalDescription(offer).catch(()=>{
            console.log(pcc.userName + ">>> setLocalDescription error" , offer);
        });
        console.log("offer 创建完成...",offer);
        offers.push({
            userName: pcc.userName,
            sdp: offer
        });
    }
    if(offers.length !== 0){
        console.log("发送 多个offer消息...",offers);
        ws.send(JSON.stringify({
            msgType: requestType.SDP_OFFER,
            msgBody: offers
        }));
    }
}

function createRTCPeerConnection(username,remoteVideo){
    const configuration = getSelectedSdpSemantics();
    console.log('RTCPeerConnection configuration:', configuration);
    const pc = new RTCPeerConnection(configuration);
    console.log(username+'>>> Created peer connection object pc');
    pc.addEventListener('icecandidate', e => onIceCandidate(e,username));
    pc.addEventListener('iceconnectionstatechange', e => onIceStateChange(pc, e));
    pc.addEventListener('track', e => {
        if (remoteVideo.srcObject !== e.streams[0]) {
            remoteVideo.srcObject = e.streams[0];
            console.log(username+'>>> pc received remote stream');
        }
    });
    localStream.getTracks().forEach(track => pc.addTrack(track, localStream));
    pcs.push({
        userName: username,
        pc: pc
    });
    return pc;
}

function createRemoteVideo(id) {
    let remoteVideo = document.createElement('video');
    remoteVideo.playsinline = true;
    remoteVideo.autoplay = true;
    remoteVideo.id=id;
    //获取body节点
    localVideo.parentNode.insertBefore(remoteVideo, localVideo.nextSibling);
    return remoteVideo;
}

