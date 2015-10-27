%INT%

/**
 * @author Petr Jecmen, Technical University of Liberec
 */
kernel void CL1D_I_V_LL_D(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global read_only int2 * subsets, global read_only float2 * subsetCenters,
    global read_only float * deformationLimits, global read_only long * deformationCounts,
    global write_only float * result,        
    const int imageWidth, const long deformationCount,
    const int subsetSize, const int subsetCount,
    const long groupCountPerFacet,
    const long subsetSubCount, const long subsetBase,
    const long deformationSubCount, const long deformationBase) 
{        
    //// ID checks    
    // subset
    const size_t groupId = get_group_id(0);
    const size_t subsetId = (groupId / groupCountPerFacet) + subsetBase;
    if (subsetId >= subsetBase + subsetSubCount || subsetId >= subsetCount) {
        return;
    }                  
    const size_t localId = get_local_id(0);
    const size_t groupSize = get_local_size(0);        
    const int subsetSize2 = (2*subsetSize + 1) * (2*subsetSize + 1);    
    // load subset to local memory   
    local int2 subsetLocal[(2*%SS%+1)*(2*%SS%+1)];
    const int baseIndexFacet = subsetId * subsetSize2;        
    if (groupSize >= subsetSize2) {
        if (localId < subsetSize2) {
            subsetLocal[localId] = subsets[baseIndexFacet + localId];
        }    
    } else {
        const int runCount = subsetSize2 / groupSize;
        int index;
        for (int i = 0; i < runCount; i++) {
            index = i*groupSize + localId;
            subsetLocal[index] = subsets[baseIndexFacet + index];
        }
        const int rest = subsetSize2 % groupSize;
        if (localId < rest) {
            index = groupSize * runCount + localId;
            subsetLocal[index] = subsets[baseIndexFacet + index];
        }
    }        
    barrier(CLK_LOCAL_MEM_FENCE);
    const int groupSubId = groupId % groupCountPerFacet;
    const int deformationId = groupSubId * groupSize + localId + deformationBase;
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