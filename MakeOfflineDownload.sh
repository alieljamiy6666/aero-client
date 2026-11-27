#!/bin/sh
java -cp "desktopRuntime/MakeOfflineDownload.jar:desktopRuntime/CompileEPK.jar" net.lax1dude.eaglercraft.v1_8.buildtools.workspace.MakeOfflineDownload "javascript/OfflineDownloadTemplate.txt" "javascript/classes.js" "javascript/assets.epk" "javascript/Aero_Client_1.12_Offline_en_US.html" "javascript/Aero_Client_1.12_Offline_International.html" "javascript/lang"
