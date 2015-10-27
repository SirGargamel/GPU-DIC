%INT%

/**
 * @author Petr Jecmen, Technical University of Liberec
 */
kernel void CL15D_pF_D(
    global read_only char * imageA, global read_only char * imageB, 
    global read_only int * subsets, global read_only float * subsetCenters,
    global read_only float * deformationLimits, global read_only long * deformationCounts,
    global write_only float * result,    
    const int imageWidth, const long deformationCount,
    const int subsetSize, const int subsetCount,
    const int subsetId,
    const long deformationSubCount, const int deformationBase)
{
    // id checks, memory init    
    %INIT%        
    // prepare coeffs    
    %DEF_C%    
    // deform subset
    %DEF%
    // compute correlation
    %CORR%
    // compute delta and store result
    %C&S%
}