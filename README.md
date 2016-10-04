# image-forensics
This project implements two well-known methods to detect modifications on images.

## ELA
Error Level Analysis works with JPEG images and analyses the compression quality of different elements of the source image. 
Usually when an image is modified, especially if part of another image was introduced, this may create a difference in the 
ability to compress which the ELA atempts to detect.

```
POST /ela
```
Parameters:
  - image - a base64 representation of the image bytes
  - ampFactor - amplification multiplier to enhance the differences
  - quantizationLevels - int between 1 and 256 used to quanitize the colors fo the image 

## Copy Move
This algorithm is an implementation of the "[Detection of Copy-Move Forgery in Digital Images](http://www.ws.binghamton.edu/fridrich/Research/copymove.pdf)" paper by Jessica Fridrich, 
David Soukal, and Jan Luká. It was modified to make it a bit more performant, but it still needs a lot of work.

```
POST /copymove
```
Parameters:
  - image - a base64 representation of the image bytes
  - maxDifference - maximum average difference between grayscale values of 8x8 blocks of pixels (e.g. 128 allows for each pixel to vary by 2 color values)
  - minShift - minimum distance between candidate repeating blocks
  - minStdDev - minimum standard deviation computed over the 8x8 block of pixels 
  - heatRadius - fade-out radius of the heat map marker
  - quantizationLevels - int between 1 and 256 used to quanitize the colors fo the image 
  
## Build
  1. Fork or download the source. 
  1. Run ``` mvn clean package ```
  1. Run ``` java -XX:-UseConcMarkSweepGC -Xmx3g -jar target/image-forensics-0.0.1.jar ``` (adjust the version and memory if necessary)
  1. Open ``` http://localhost:8080/ ```

