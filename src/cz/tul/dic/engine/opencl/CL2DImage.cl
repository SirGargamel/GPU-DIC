constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

inline int interpolate(const float x, const float y, read_only image2d_t image) {
    const float ix = floor(x);
    const float dx = x - ix;
    
    const float iy = floor(y);
    const float dy = y - iy;

    const float intensity = 
        read_imageui(image, sampler, (float2)(ix, iy)).x * (1 - dx) * (1 - dy)
        + read_imageui(image, sampler, (float2)(ix+1, iy)).x * dx * (1 - dy)
        + read_imageui(image, sampler, (float2)(ix, iy+1)).x * (1 - dx) * dy
        + read_imageui(image, sampler, (float2)(ix+1, iy+1)).x * dx * dy;               

    return intensity;    
}

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
        
        deformedFacet[i2] = x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * dx + deformations[baseIndexDeformation + 4] * dy;                    
        deformedFacet[i2 + 1] = y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * dx + deformations[baseIndexDeformation + 5] * dy; 
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
        
        deformedI[i] = interpolate(deformedFacet[i2], deformedFacet[i2+1], imageB);        
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
    for (int i = 0; i < facetSize2; i++) {
        index = baseIndexFacet + i*2;        
        resultVal += facetI[i] * deformedI[i];
    }
    resultVal /= sqrt(deltaF) * sqrt(deltaG);  
    
    //store result
    index = facetId * deformationCount + deformationId;
    result[index] = resultVal;    
}