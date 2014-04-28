constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

%INT%

kernel void CL1D_I_V_LL_MC_D(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global read_only int2 * facets, global read_only float2 * facetCenters,
    global read_only float * deformations,
    global write_only float * result,        
    const int imageWidth, const int deformationCount,
    const int facetSize, const int facetCount,
    const int groupCountPerFacet,
    const int facetSubCount, const int facetBase,
    const int deformationSubCount, const int deformationBase) 
{        
    //// ID checks    
    // facet
    const size_t groupId = get_group_id(0);
    const size_t facetId = (groupId / groupCountPerFacet) + facetBase;
    if (facetId >= facetBase + facetSubCount || facetId >= facetCount) {
        return;
    }        
    // deformation    
    const int groupSubId = groupId % groupCountPerFacet;
    const size_t localId = get_local_id(0);
    const size_t groupSize = get_local_size(0);
    const int deformationId = groupSubId * groupSize + localId;    
    // index computation
    const int facetSize2 = facetSize * facetSize;
    const int baseIndexDeformation = (deformationBase + deformationId) * %DEF_D%;
    // load facet to local memory    
    local int2 facetLocal[-1*-1];
    int index;
    if (groupSize >= facetSize2) {
        if (localId < facetSize2) {
            index = localId*facetCount + facetId;            
            facetLocal[localId] = facets[index];            
        }    
    } else {
        const int runCount = facetSize2 / groupSize;
        int id;
        for (int i = 0; i < runCount; i++) {
            id = i*groupSize + localId;
            index = id*facetCount + facetId; 
            facetLocal[id] = facets[index];            
        }
        const int rest = facetSize2 % groupSize;
        if (localId < rest) {
            id = groupSize * runCount + localId;
            index = id*facetCount + facetId;            
            facetLocal[id] = facets[index];            
        }
    }        
    barrier(CLK_LOCAL_MEM_FENCE);
    if (deformationId >= deformationBase + deformationSubCount || deformationId >= deformationCount) {
        return;
    }
    // deform facet
    float2 deformedFacet[-1*-1];
    float2 coords, def; 
    for (int i = 0; i < facetSize2; i++) {
        coords = convert_float2(facetLocal[i]);       

        def = coords - facetCenters[facetId];
        
//        deformedFacet[i] = (float2)(
//            coords.x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * def.x + deformations[baseIndexDeformation + 4] * def.y, 
//            coords.y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * def.x + deformations[baseIndexDeformation + 5] * def.y);
        deformedFacet[i] = (float2)(%DEF_X%, %DEF_Y%);
    }
    // compute correlation using ZNCC
    float deformedI[-1*-1];
    float facetI[-1*-1];
    float meanF = 0;
    float meanG = 0; 
    for (int i = 0; i < facetSize2; i++) {
        facetI[i] = read_imageui(imageA, sampler, facetLocal[i]).x;
        meanF += facetI[i];
        
        deformedI[i] = interpolate(deformedFacet[i], imageB);
        meanG += deformedI[i];
    } 
    meanF /= (float) facetSize2;
    meanG /= (float) facetSize2;    
    
    float deltaF = 0;
    float deltaG = 0;   
    for (int i = 0; i < facetSize2; i++) {                                             
        facetI[i] -= meanF;
        deltaF += facetI[i] * facetI[i];
                        
        deformedI[i] -= meanG;
        deltaG += deformedI[i] * deformedI[i];
    }    
    
    float resultVal = 0;           
    if (deltaF != 0 && deltaG != 0) {
        for (int i = 0; i < facetSize2; i++) {            
            resultVal += facetI[i] * deformedI[i];
        }
        resultVal /= sqrt(deltaF) * sqrt(deltaG);  
    }    
    
    //store result    
    result[facetId * deformationCount + deformationId] = resultVal;    
}