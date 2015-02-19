# MC-UnusedChunksDeleter
Deletes chunks in region files with InhabitedTime less than the input value.

https://github.com/Sladki/MC-UnusedChunksDeleter/raw/master/jar/UnusedChunksDeleter.jar

How to use: put in the server or save directory and launch

java -jar UnusedChunksDeleter.jar <time> <threads>

Where <time> is time in ticks, chunks with InhabitedTime lesser than will be deleted, and <threads> is number of threads (optional).
Clean region files will be at 'world/region/clean' directory, source region files will not be changed. Copy files in 'world/region' directory to another folder (your backup) and replace with "clean" region files.

This program uses Minecraft AnvilConverter files.
https://assets.minecraft.net/12w07a/Minecraft.AnvilConverter.zip