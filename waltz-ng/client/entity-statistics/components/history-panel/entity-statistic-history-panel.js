import _ from 'lodash';
import d3 from 'd3';
import {variableScale} from '../../../common/colors';


const bindings = {
    history: '<',
    definition: '<'
};


const template = require('./entity-statistic-history-panel.html');


const dateFormatter = d3
    .time
    .format('%a %d %b %Y');


function prepareData(data = []) {
    return _.chain(data)
        .flatMap(
            d => _.map(d.tallies, t => {
                return {
                    series: t.id,
                    count: t.count,
                    date: new Date(d.lastUpdatedAt)
                };
            }))
        .orderBy(d => d.date)
        .value();
}


function getOutcomeIds(data = []) {
    return _.chain(data)
        .flatMap('tallies')
        .map('id')
        .uniq()
        .value();
}


function prepareStyles(data = []) {
    const reducer = (acc, outcomeId) => {
        acc[outcomeId] = { color: variableScale(outcomeId) };
        return acc;
    };
    return _.reduce(
        getOutcomeIds(data),
        reducer,
        {});
}


function findRelevantStats(history = [], d) {
    const soughtTime = new Date(d).getTime();
    return _.find(
        history,
        t => soughtTime === new Date(t.lastUpdatedAt).getTime());
}


function lookupStatColumnName(displayNameService, definition) {
    return definition
        ? displayNameService.lookup('rollupKind', definition.rollupKind)
        : 'Value';
}


function controller($scope, displayNameService) {
    const vm = this;

    const highlight = (d) => {
        vm.options = Object.assign({}, vm.options, { highlightedDate: d });
        const relevantStats = findRelevantStats(vm.history, d);
        if (relevantStats) {
            vm.selected = relevantStats;
            vm.selected.dateString = dateFormatter(d);
        }
    };

    vm.options = {
        highlightedDate: null,
        onHover: (d) => $scope.$applyAsync(() => highlight(d))
    };

    vm.$onChanges = () => {
        vm.selected  = null;
        vm.points = prepareData(vm.history);
        vm.styles = prepareStyles(vm.history);
        vm.statColumnName = lookupStatColumnName(displayNameService, vm.definition);
    };
}


controller.$inject = [
    '$scope',
    'WaltzDisplayNameService'
];


const component = {
    bindings,
    template,
    controller
};


export default component;