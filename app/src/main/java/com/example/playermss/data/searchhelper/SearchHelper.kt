package com.example.playermss.data.searchhelper

import com.example.playermss.data.MediaTrackData

interface SearchHelper {

    var overrunTop: Boolean
    var overrunBottom: Boolean
    var repeatMode: Int

    fun clearOverruns(){
        overrunTop = false
        overrunBottom = false
    }

    fun checkOverrunsFromTopToBottom():Boolean {
        var overrun = false
        if(overrunTop){
            overrunTop = false
            overrunBottom = true
            overrun = true
        }else{
            if(overrunBottom)
            {
                overrunBottom = false
            }
        }
        return overrun
    }

    fun checkOverrunsFromBottomToTop():Boolean {
        var overrun = false
        if(overrunBottom){
            overrunTop = true
            overrunBottom = false
            overrun = true
        }else{
            if(overrunTop){
                overrunTop = false
            }
        }
        return overrun
    }

    fun getMediaTrackDataForCode(code: Int): MediaTrackData?
    fun getPrev(mediaTrackData: MediaTrackData?): MediaTrackData?
    fun getNext(mediaTrackData: MediaTrackData?): MediaTrackData?
}