inline int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

%INT%

/**
 * @author Petr Jecmen, Technical University of Liberec
 */
kernel void CL2D_Int_D(
    global read_only int * imageA, global read_only int * imageB, 
    global read_only int * facets, global read_only float * facetCenters,
    global read_only float * deformationLimits, global read_only int * deformationCounts,
    global write_only float * result,    
    const int imageWidth, const int deformationCount,
    const int facetSize, const int facetCount,
    const int groupCountPerFacet,
    const int facetSubCount, const int facetBase,
    const int deformationSubCount, const int deformationBase) 
{        
    // id checks    
    const size_t facetId = facetBase + get_global_id(0);
    if (facetId >= facetBase + facetSubCount || facetId >= facetCount) {
        return;
    } 
    const size_t deformationId = get_global_id(1);
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
    int index, i2, x, y;
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
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;
                
        // facet is just array of int coords              
        facetI[i] = imageA[computeIndex(facets[index], facets[index + 1], imageWidth)];
        meanF += facetI[i];
                               
        deformedI[i] = interpolate(deformedFacet[i2], deformedFacet[i2 + 1], imageB, imageWidth);
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