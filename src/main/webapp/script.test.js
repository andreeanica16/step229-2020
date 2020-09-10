const {getTotalCosts, getAverageCosts} = require('./script');

var testAggregatedCostInput = '{ "testData" : [' +
  '{ "price":0.014105724489430654 },' +
  '{ "price":0.014105724489430655 },' +
  '{ "price":0.014105724489430656 }]}';
testAggregatedCostInput = JSON.parse(testAggregatedCostInput);


test('Test getTotalCosts', () => {
  const result = [["Category", "Total Cost"],["testData", 0.04231717346829197]]
  expect(getTotalCosts(testAggregatedCostInput)).toStrictEqual(result);
});

test('Test getAverageCosts', () => {
  const result = [["Category", "Average Cost"],["testData", 0.014105724489430656]]
  expect(getAverageCosts(testAggregatedCostInput)).toStrictEqual(result);
});