constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

%INT%

kernel void CL2DImage(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global read_only int * facets, global read_only float * facetCenters,
    global read_only float * deformations,
    global write_only float * result,    
    const int imageWidth, const int deformationCount,
    const int facetSize, const int facetCount) 
{    
    // id checks    
    const size_t facetId = get_global_id(0);
    if (facetId >= facetCount) {
        return;
    }        
    const size_t deformationId = get_global_id(1);
    if (deformationId >= deformationCount) {
        return;
    }
    // index computation
    const int facetSize2 = facetSize * facetSize;
    const int facetCoordCount = facetSize2 * 2;    
    const int baseIndexFacet = facetId * facetCoordCount; 
    const int baseIndexFacetCenter = facetId * 2;
    const int baseIndexDeformation = deformationId * %DEF_D%;
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
        
//        deformedFacet[i2] = x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * dx + deformations[baseIndexDeformation + 4] * dy;                    
//        deformedFacet[i2 + 1] = y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * dx + deformations[baseIndexDeformation + 5] * dy; 
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
        facetI[i] = read_imageui(imageA, sampler, (int2)(facets[index], facets[index + 1])).x;        
        meanF += facetI[i];
        
        deformedI[i] = interpolate((float2)(deformedFacet[i2], deformedFacet[i2+1]), imageB);        
        meanG += deformedI[i];
    } 
    meanF /= (float) facetSize2;
    meanG /= (float) facetSize2;    
    
    float deltaF = 0;
    float deltaG = 0;   
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;
                             
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