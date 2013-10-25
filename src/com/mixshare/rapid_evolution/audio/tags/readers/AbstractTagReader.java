package com.mixshare.rapid_evolution.audio.tags.readers;

import com.mixshare.rapid_evolution.audio.tags.TagConstants;
import com.mixshare.rapid_evolution.audio.tags.TagReader;

public abstract class AbstractTagReader implements TagReader, TagConstants {

    public abstract boolean isFileSupported();    
    public abstract String getAlbum();
    public abstract String getAlbumCoverFilename();
    public abstract String getArtist();
    public abstract Integer getBeatIntensity();    
    public abstract Integer getBpmAccuracy();
    public abstract Float getBpmStart();
    public abstract Float getBpmEnd();
    public abstract String getCatalogId();    
    public abstract String getComments();
    public abstract String getContentGroupDescription();
    public abstract String getContentType();
    public abstract String getEncodedBy();
    public abstract String getFilename();    
    public abstract String getFileType();
    public abstract String getGenre();
    public abstract Integer getKeyAccuracy();
    public abstract String getKeyStart();
    public abstract String getKeyEnd();
    public abstract String getLanguages();
    public abstract String getLyrics();    
    public abstract String getPublisher();
    public abstract Integer getRating();
    public abstract String getRemix();
    public abstract Float getReplayGain();
    public abstract Integer getSizeInBytes();
    public abstract String[] getStyles();
    public abstract String getTime();
    public abstract String getTimeSignature();    
    public abstract String getTitle();
    public abstract String getTrack();
    public abstract Integer getTotalTracks();
    public abstract String getUser1();    
    public abstract String getUser2();    
    public abstract String getUser3();    
    public abstract String getUser4();    
    public abstract String getYear();
    
}
