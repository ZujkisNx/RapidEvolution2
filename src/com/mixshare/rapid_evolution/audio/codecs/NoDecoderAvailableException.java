package com.mixshare.rapid_evolution.audio.codecs;

public class NoDecoderAvailableException extends Exception {

    private String filename;
    
    public NoDecoderAvailableException(String filename) {
        this.filename = filename;
    }
    
    public String toString() {
        return "no audio decoder available for file=" + filename;
    }
    
}
