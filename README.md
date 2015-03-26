# GPU-DIC
*Digital Image Correlation on GPU*

GPU-DIC is a tool for video analysis. The algorithm is best suited for analysis of recordings of material stress tests. 
The analysis is done using DIC algorithm. The computation is carried out on GPU, which offers better speeds 
than CPU implementations while achieving the same precision.

## Main characteristics
  * Computation done using OpenCL - computation done on GPU (or on CPU where no GPU is available)
  * Deformation and strain field computation
  * Input in form of video or image series
  * No limitation on camera FPS
  * Many output options - direct vizualization, image / video export, numerical outputs in form of CSV
  * Support both for GUI controlled and script controlled computation

The code is freely available for reference, use of the application usage is license limited/ If you would like to use the application, please contact petr.jecmen@tul.cz for license info.
