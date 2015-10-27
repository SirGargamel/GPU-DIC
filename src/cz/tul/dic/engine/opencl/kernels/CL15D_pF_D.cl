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
    // id checks       
    const size_t deformationId = deformationBase + get_global_id(0);    
    if (deformationId >= deformationBase + deformationSubCount || deformationId >= deformationCount) {
        return;
    }
    // prepare coeffs
    float deformation[%DEF_D%];
    %DEF_C%    
    // deform subset
    %DEF%
    // compute correlation
    %CORR%
    // compute delta and store result
    %C&S%
}