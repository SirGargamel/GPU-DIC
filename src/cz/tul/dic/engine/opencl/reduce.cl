kernel void reduce(
        global float * buffer,
        local float * scratch,        
        global float * result,        
        const int groupSize, 
        const int groupId) {
    const int base = groupId * groupSize;
    int global_index = get_global_id(0);
    float accumulator = -INFINITY;
    // Loop sequentially over chunks of input vector
    while (global_index < groupSize) {
        accumulator = fmax(accumulator, buffer[base + global_index]);
        global_index += get_global_size(0);
    }

    // Perform parallel reduction
    int local_index = get_local_id(0);
    scratch[local_index] = accumulator;
    barrier(CLK_LOCAL_MEM_FENCE);
    for(int offset = get_local_size(0) / 2; offset > 0; offset = offset / 2) {
        if (local_index < offset) {
            scratch[local_index] = fmax(scratch[local_index], scratch[local_index + offset]);
        }
        barrier(CLK_LOCAL_MEM_FENCE);
    }

    if (local_index == 0) {
        result[groupId] = scratch[0];
    }
/*
    result[global_index] = accumulator;
*/
}