constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;

%INT%

/**
 * @author Petr Jecmen, Technical University of Liberec
 */
kernel void CL1D_I_V_LL_MC_D(
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
    local int2 subsetLocal[(2*-1+1)*(2*-1+1)];
    int index;
    if (groupSize >= subsetSize2) {
        if (localId < subsetSize2) {            
            subsetLocal[localId] = subsets[localId*subsetCount + subsetId];            
        }    
    } else {
        const int runCount = subsetSize2 / groupSize;
        int id;
        for (int i = 0; i < runCount; i++) {
            id = i*groupSize + localId;
            index = id*subsetCount + subsetId; 
            subsetLocal[id] = subsets[index];            
        }
        const int rest = subsetSize2 % groupSize;
        if (localId < rest) {
            id = groupSize * runCount + localId;
            index = id*subsetCount + subsetId;            
            subsetLocal[id] = subsets[index];            
        }
    }        
    barrier(CLK_LOCAL_MEM_FENCE);
    const int groupSubId = groupId % groupCountPerFacet;
    const int deformationId = groupSubId * groupSize + localId + deformationBase;
    if (deformationId >= deformationBase + deformationSubCount || deformationId >= deformationCount) {
        return;
    }
    float deformation[%DEF_D%];
    %DEF_C%
    // deform subset
    float2 deformedFacet[(2*-1+1)*(2*-1+1)];
    float2 coords, def; 
    for (int i = 0; i < subsetSize2; i++) {
        coords = convert_float2(subsetLocal[i]);       

        def = coords - subsetCenters[subsetId];
        
        deformedFacet[i] = (float2)(%DEF_X%, %DEF_Y%);
    }
    
    %CORR%    
    
    %C&S%    
}