// Adds the permissions background audio and local songs need. Safe to run repeatedly.
import {readFileSync,writeFileSync,existsSync,copyFileSync,mkdirSync} from "node:fs";
import {dirname,join} from "node:path";
import {fileURLToPath} from "node:url";
const p=join(dirname(fileURLToPath(import.meta.url)),"..","android","app","src","main","AndroidManifest.xml");
if(!existsSync(p)){console.log("no android project yet");process.exit(0)}
let x=readFileSync(p,"utf8");
const perms=[
 '<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />',
 '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />',
 '<uses-permission android:name="android.permission.WAKE_LOCK" />',
 '<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />',
 '<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />',
 '<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />'
];
let add="";for(const l of perms){const n=l.match(/name="([^"]+)"/)[1];if(!x.includes(n))add+="    "+l+"\n"}
if(add)x=x.replace(/<\/manifest>\s*$/,add+"</manifest>");
writeFileSync(p,x);console.log(add?"permissions added":"permissions already present");

// Firebase (push notifications): put your google-services.json in native/ and it is copied into the app module.
const gs=join(dirname(fileURLToPath(import.meta.url)),"..","google-services.json"),gd=join(dirname(p),"..","..","google-services.json");
if(existsSync(gs)){copyFileSync(gs,gd);console.log("google-services.json copied (push notifications on)")}else console.log("no native/google-services.json yet - push notifications stay off in the APK until you add it (see PUSH.md)");

// Device music: copy the MediaStore plugin into the app and register it in MainActivity.
const nat=join(dirname(fileURLToPath(import.meta.url)),".."),jd=join(nat,"android","app","src","main","java","com","ubayanda","milo");
mkdirSync(jd,{recursive:true});
for(const f of ["MiloMediaPlugin.java","MiloAudioPlugin.java","MiloPlayService.java"])copyFileSync(join(nat,"plugin",f),join(jd,f));
writeFileSync(join(jd,"MainActivity.java"),`package com.ubayanda.milo;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(MiloMediaPlugin.class);
        registerPlugin(MiloAudioPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
`);
console.log("device music plugin installed");

// Native audio engine: background playback service in the manifest + Media3 libraries in the app module.
{let m=readFileSync(p,"utf8");
 if(!m.includes("MiloPlayService")){
  m=m.replace("</application>",`    <service android:name="com.ubayanda.milo.MiloPlayService" android:exported="true" android:foregroundServiceType="mediaPlayback">
            <intent-filter><action android:name="androidx.media3.session.MediaSessionService" /></intent-filter>
        </service>
    </application>`);writeFileSync(p,m);console.log("playback service added to manifest")}else console.log("playback service already in manifest")
 const g=join(dirname(p),"..","..","build.gradle");
 if(existsSync(g)){let b=readFileSync(g,"utf8");
  if(!b.includes("androidx.media3")){b=b.replace(/dependencies\s*\{/,`dependencies {
    implementation "androidx.media3:media3-exoplayer:1.4.1"
    implementation "androidx.media3:media3-session:1.4.1"`);writeFileSync(g,b);console.log("Media3 libraries added to app/build.gradle")}else console.log("Media3 libraries already present")}
 else console.log("app/build.gradle not found - native audio engine will be skipped")}
