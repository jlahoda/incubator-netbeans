<?php
$foo = array("lllllooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnggggggggggggggg", "lllllooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnggggggggggggggg");

$myLongArray = array('element1', 'element2', 'element3', 'element4',
    'element5', 'element6', 'element7', 'element8', 'element9', 'element10');

class F {
    function foreachEample() {
        $arr = array( 1, 2, 3, 4, "b" => 5, "a" => 6);
        foreach ($arr as &$value) {
            $value = (int) $value * 2;
        }
    }
}

$test = array(
    // P1
    true,
    // P2
    false,
    # P3
    // P4
    3 => 'test',
    // P5
    4 => 'test2'
);
?>