package com.example.musiclog.domain.model

data class Music(
    val id : String, //Youtube Video ID 또는 고유 식별자
    val title : String,
    val artist : String,
    val albumArtUrl : String, //Youtube API에서 제공하는 앨범 커버 URL
    val customAlbumArtUri : String? = null, //사용자가 직접 수정한 로컬 이미지의 URI (없을 경우 null)
    val playCount : Int, //이 앱의 핵심 기능인 누적 재생 횟수 체크를 위한 변수 (Default=0)
    val lastPlayedTimeStamp : Long = System.currentTimeMillis() //리캡 분석을 위한 최근 재생 시간 (Unix Timestamp)
)
