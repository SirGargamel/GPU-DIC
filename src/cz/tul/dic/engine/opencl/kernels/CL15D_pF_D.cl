inline int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

%INT%

/**
 * @author Petr Jecmen, Technical University of Liberec
 */
kernel void CL15D_pF_D(
    global read_only int * imageA, global read_only int * imageB, 
    global read_only int * facets, global read_only float * facetCenters,
    global read_only float * deformationLimits, global read_only int * deformationCounts,
    global write_only float * result,    
    const int imageWidth, const int deformationCount,
    const int facetSize, const int facetCount,
    const int facetId,
    const int deformationSubCount, const int deformationBase)
{
    // id checks       
    const size_t deformationId = get_global_id(0);    
    if (deformationId >= deformationBase + deformationSubCount || deformationId >= deformationCount) {
        return;
    }        
    float deformation[%DEF_D%];
    %DEF_C%
    // index computation
    const int facetSize2 = facetSize * facetSize;
    const int facetCoordCount = facetSize2 * 2;
    const int baseIndexFacet = facetId * facetCoordCount;         
    const int baseIndexFacetCenter = facetId * 2; 
    const int baseIndexDeformation = deformationId * 6;
    // deform facet
    float deformedFacet[-1*-1*2];    
    int i2, index, x, y;
    float dx, dy;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;        
        
        x = facets[index];
        y = facets[index+1];                

        dx = x - facetCenters[baseIndexFacetCenter];
        dy = y - facetCenters[baseIndexFacetCenter + 1]; 
        
        deformedFacet[i2] = %DEF_X%;
        deformedFacet[i2 + 1] = %DEF_Y%;
    }
    // compute correlation using ZNCC
    float deformedI[-1*-1];
    float facetI[-1*-1];
    float meanF = 0;
    float meanG = 0; 
    float val;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;    
        index = baseIndexFacet + i2;      
                
        // facet is just array of int coords        
        val = imageA[computeIndex(facets[index], facets[index + 1], imageWidth)];
        facetI[i] = val;
        meanF += val;
        
        val = interpolate(deformedFacet[i2], deformedFacet[i2 + 1], imageB, imageWidth);                        
        deformedI[i] = val;
        meanG += val;
    } 
    meanF /= (float) facetSize2;
    meanG /= (float) facetSize2;
    
    float deltaF = 0;
    float deltaG = 0;    
    for (int i = 0; i < facetSize2; i++) {                                     
        val = facetI[i] - meanF;
        facetI[i] = val;
        deltaF += val * val;
                
        val = deformedI[i] - meanG;
        deformedI[i] = val;
        deltaG += val * val;
    }  
    
    val = 0;
    for (int i = 0; i < facetSize2; i++) {                    
        val += facetI[i] * deformedI[i];
    }    
    val /= sqrt(deltaF) * sqrt(deltaG);
    
    //store result    
    result[deformationId] = val;
}