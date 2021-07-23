# Report Sender
Displays a window to contact the developer when a mod crashes Minecraft.

## How to use
1. Include the source of SenderDownloader into your mod by using Shadow, etc.

   **Warning: Do not include ReportSender. It may conflict with other mods that use ReportSender.**
2. Configure TweakClass in META-INF/MANIFEST.MF.
   Please refer to the jar/manifest section of SenderDownloader/build.gradle.
3. Create the xyz.fmdc.reportsender.properties package in your mod and create a <any name>.properties file.
   See src/main/java/xyz/fmdc/reportsender/properties/ReportSender.properties.sample file for the format. Add the number of destinations.


This program uses Crash Report Publisher, a library developed by anatawa12. licensed under MIT License
see licenses/LICENSE-crash-report-publisher.txt