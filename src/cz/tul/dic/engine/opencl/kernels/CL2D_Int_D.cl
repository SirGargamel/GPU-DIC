inline int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

%INT%

/**
 * @author Petr Jecmen, Technical University of Liberec
 */
kernel void CL2D_Int_D(
    global read_only char * imageA, global read_only char * imageB, 
    global read_only int * subsets, global read_only float * subsetCenters,
    global read_only float * deformationLimits, global read_only long * deformationCounts,
    global write_only float * result,    
    const int imageWidth, const long deformationCount,
    const int subsetSize, const int subsetCount,
    const long groupCountPerFacet,
    const long subsetSubCount, const long subsetBase,
    const long deformationSubCount, const long deformationBase) 
{        
    // id checks    
    const size_t subsetId = subsetBase + get_global_id(0);
    if (subsetId >= subsetBase + subsetSubCount || subsetId >= subsetCount) {
        return;
    } 
    const size_t deformationId = deformationBase + get_global_id(1);
    if (deformationId >= deformationBase + deformationSubCount || deformationId >= deformationCount) {
        return;
    }
    float deformation[%DEF_D%];
    %DEF_C%
    // index computation
    const int subsetSize2 = (2*subsetSize + 1) * (2*subsetSize + 1);
    const int subsetCoordCount = subsetSize2 * 2;
    const int baseIndexFacet = subsetId * subsetCoordCount;         
    const int baseIndexFacetCenter = subsetId * 2;
    const int baseIndexDeformation = deformationId * 6;
    // deform subset    
    float deformedFacet[(2*-1+1)*(2*-1+1)*2];    
    int index, i2, x, y;
    float dx, dy;
    for (int i = 0; i < subsetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;        
        
        x = subsets[index];
        y = subsets[index+1];

        dx = x - subsetCenters[baseIndexFacetCenter];
        dy = y - subsetCenters[baseIndexFacetCenter + 1];
        
        deformedFacet[i2] = %DEF_X%;
        deformedFacet[i2 + 1] = %DEF_Y%;
    }
    
    %CORR%
    
    %C&S%
}