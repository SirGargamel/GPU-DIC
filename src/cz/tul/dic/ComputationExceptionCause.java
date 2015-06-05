/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;


/**
 *
 * @author Petr Jecmen
 */
public enum ComputationExceptionCause {

    NOT_ENOUGH_ROIS,
    FIXTURES_SHIFT_MISMATCH,
    ILLEGAL_TASK_DATA,
    OPENCL_ERROR,
    ILLEGAL_CONFIG,
    MEMORY_ERROR,
    IO;

}
