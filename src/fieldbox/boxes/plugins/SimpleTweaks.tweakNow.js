// %%NO_OVERLOADS%%
var HandlesForFLines = Java.type('fieldbox.boxes.plugins.HandlesForFLines');
var DraggableNode = Java.type('fieldbox.boxes.plugins.HandlesForFLines.DraggableNode');

prop = "_simpleTweaks";

_.once[prop] = () => "//%%NO_OVERLOADS%%\n";

_.exec(_[prop]);

for (var e of _.lines.entrySet()) {
    let name = e.getKey();
    let val = e.getValue();
    if (val.tweaky)
        for (var n of e.getValue().nodes)
            d = new DraggableNode(_, val, n,
                s => {
                    desc = s.describe("HandlesForFLines", "_.lines['" + name+"']");
                    _[prop] += desc + "\n";
                    _.next.update = () => {
                        _()
                    }
                })
}
