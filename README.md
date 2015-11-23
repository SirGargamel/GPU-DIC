# GPU-DIC
*Digital Image Correlation on GPU*

GPU-DIC is a tool for video analysis. The algorithm is best suited for analysis of recordings of material stress tests. The analysis is done using DIC algorithm. The computation is carried out on GPU, which offers better speeds 
than CPU implementations while achieving the same precision.

## Main characteristics
  * Computation done using OpenCL - computation done on GPU (or on CPU where no GPU is available)
  * Deformation and strain field computation
  * Input in form of video or image series
  * No limitation on camera FPS
  * Support for dynamic mesh generation
  * Automatic input pre-filtering for better result quality
  * Multiple orders of deformations - Zero, First and Second
  * Multiple correlation criterions - ZNCC, ZNSSD, WZNSSD
  * Multiple solvers available - CoarseFine, Newton-Raphson, SPGD
  * Many output options - direct vizualization, image / video export, numerical outputs in form of CSV
  * Support both for GUI controlled and script controlled computation
  
## Theoretical overview of implemented functions
  * Two-dimensional digital image correlation for in-plane displacement and strain measurement: a review -- Bing Pan, Kemao Qian, Huimin Xie and Anand Asundi
  * Image pre-filtering for measurement error reduction in digital image correlation -- Yihao Zhou, Chen Sun, Yuntao Song, Jubing Chen
  * Deformation Measurements by Digital Image Correlation: Implementation of a Second-order Displacement Gradient -- H. Lu and P. D. Cary
  * Digital image correlation using iterative least squares and pointwise least squares for displacement field and strain field measurements -- Bing Pan, Anand Asundi, Huimin Xie, Jianxin Gao
  * Accurate displacement measurement via a self-adaptive digital image correlation method based on a weighted ZNSSD criterion -- Yuan Yuan, Jianyong Huang, Xiaoling Peng, Chunyang Xiong, Jing Fang, Fan Yuan
  * A self-adaptive sampling digital image correlation algorithm for accurate displacement measurement -- Yuan Yuan, Jianyong Huang, Jing Fang, Fan Yuan, Chunyang Xiong
  * A novel coarse-fine search scheme for digital image correlation method -- Zhi-Feng Zhang, Yi-Lan Kang , Huai-Wen Wang, Qing-Hua Qin, Yu Qiu, Xiao-Qi Li
  * Digital Image Correlation Using Stochastic Parallel-Gradient-Descent Algorithm -- X. Long, S. Fu, Z. Qi, X. Yang, Qifeng Yu        

The code is freely available for reference, use of the application usage is license limited. If you would like to use the application, please contact petr.jecmen@tul.cz for licensing info.
