package com.datagram;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;

import com.google.gson.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.ibm.watson.developer_cloud.http.HttpMediaType;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;




public class Server extends Thread {

AudioInputStream audioInputStream;
static AudioInputStream ais;
static AudioFormat format;
static boolean status = true;
static int port = 9868;
static int sampleRate = 44100;
static int bufferSize = 2200;
static DataLine.Info dataLineInfo;
static SourceDataLine sourceDataLine;
static SpeechToText service;
static private DatagramSocket serverSocket;
static byte[] receiveData = new byte[4096];
static byte[] sendData = new byte[4096];
static int count = 1000000;

public static void main(String[] args) throws Exception {
	service = new SpeechToText();
	service.setUsernameAndPassword("e814a8a3-c752-4292-a3e9-dde4b298546f", "xvaZCGt6TniG");

    try {
		serverSocket = new DatagramSocket(port);
	} catch (SocketException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

    /**
     * Formula for lag = (byte_size/sample_rate)*2
     * Byte size 9728 will produce ~ 0.45 seconds of lag. Voice slightly broken.
     * Byte size 1400 will produce ~ 0.06 seconds of lag. Voice extremely broken.
     * Byte size 4000 will produce ~ 0.18 seconds of lag. Voice slightly more broken then 9728.
     */


    format = new AudioFormat(sampleRate, 16, 1, true, true);
    dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
    try {
		sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
	} catch (LineUnavailableException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    try {
		sourceDataLine.open(format);
	} catch (LineUnavailableException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    sourceDataLine.start();




    while (status) {
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        try {
			serverSocket.receive(receivePacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ByteArrayInputStream baiss = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
        InetAddress address = receivePacket.getAddress();
        int port = receivePacket.getPort();

        
        count = count + 1;
        System.out.println("yep");
        String capitalizedSentence = Integer.toString(count);
        sendData = capitalizedSentence.getBytes();
        
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        
        try {
			serverSocket.send(sendPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println(count);
        //ais = new AudioInputStream(receivePacket.getData(), format, receivePacket.getLength());
        //AudioInputStream oAIS = AudioSystem.getAudioInputStream(baiss);
        toIBM(baiss);
        toSpeaker(receivePacket.getData());
        Thread.sleep(1000);
    }
    sourceDataLine.drain();
    sourceDataLine.close();
}


    public static void toSpeaker(byte soundbytes[]) {
        try {
            sourceDataLine.write(soundbytes, 0, soundbytes.length);
        } catch (Exception e) {
            System.out.println("Not working in speakers...");
            e.printStackTrace();
        }
    }


    public static void toIBM(InputStream aisss) {
        try {
        	RecognizeOptions options = new RecognizeOptions.Builder()
            		.audio(aisss)
            		.interimResults(true)
            		.timestamps(false)
            		.wordConfidence(false)
            		.smartFormatting(true)
            		//.inactivityTimeout(5) // use this to stop listening when the speaker pauses, i.e. for 5s
            		.contentType("audio/l16;rate=" + sampleRate)
            		.build();

            	service.recognizeUsingWebSocket(options, new BaseRecognizeCallback() {
            	@Override
            	public void onTranscription(SpeechRecognitionResults speechResults) {
            		System.out.println(speechResults);
            	}
            	});
                System.out.println("Listening to your voice for the next 30s...");
                Thread.sleep(30 * 1000);
   
        } catch (Exception e) {
            System.out.println("Not working in speakers...");
            e.printStackTrace();
        }
    }
    

    
    
}