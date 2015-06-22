constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;

%INT%

/**
 * @author Petr Jecmen, Technical University of Liberec
 */
kernel void CL1D_I_V_LL_D(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global read_only int2 * subsets, global read_only float2 * subsetCenters,
    global read_only float * deformationLimits, global read_only int * deformationCounts,
    global write_only float * result,        
    const int imageWidth, const int deformationCount,
    const int subsetSize, const int subsetCount,
    const int groupCountPerFacet,
    const int subsetSubCount, const int subsetBase,
    const int deformationSubCount, const int deformationBase) 
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
    // compute correlation using ZNCC
    float deformedI[(2*-1+1)*(2*-1+1)];
    float subsetI[(2*-1+1)*(2*-1+1)];
    float meanF = 0;
    float meanG = 0; 
    for (int i = 0; i < subsetSize2; i++) {
        subsetI[i] = read_imageui(imageA, sampler, subsetLocal[i]).x;
        meanF += subsetI[i];
        
        deformedI[i] = interpolate(deformedFacet[i].x, deformedFacet[i].y, imageB);
        meanG += deformedI[i];
    } 
    meanF /= (float) subsetSize2;
    meanG /= (float) subsetSize2;    
    
    float deltaF = 0;
    float deltaG = 0;   
    for (int i = 0; i < subsetSize2; i++) {                                             
        subsetI[i] -= meanF;
        deltaF += subsetI[i] * subsetI[i];
                        
        deformedI[i] -= meanG;
        deltaG += deformedI[i] * deformedI[i];
    }    
    
    float resultVal = 0;           
    if (deltaF != 0 && deltaG != 0) {
        for (int i = 0; i < subsetSize2; i++) {            
            resultVal += subsetI[i] * deformedI[i];
        }
        resultVal /= sqrt(deltaF) * sqrt(deltaG);  
    }    
    
    //store result    
    result[subsetId * deformationCount + deformationId] = resultVal;
}